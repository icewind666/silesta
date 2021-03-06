import datetime

import flask
import postgresql
import json
import locale

import pytz
from dateutil.relativedelta import relativedelta
from flask import Flask, render_template, request

app = Flask(__name__)

# load json config file
with open('web.config') as json_file:
    conf = json.load(json_file)


@app.route('/balance_data', methods=['GET'])
def balance():
    with postgresql.open(conf["postgresql"]) as db:
        today_date = datetime.date.today()
        result_date_start = today_date.replace(day=1)
        result_date_end = result_date_start + relativedelta(months=1)

        ops = db.query("SELECT bank_operations.amount,bank_operations.\"desc\",bank_operations.cat_name,"
                       "bank_operations.is_income,bank_operations.op_date FROM   public.bank_operations"
                       " where bank_operations.op_date between '{}' and '{}' "
                       " order by bank_operations.op_date ASC;".format(result_date_start, result_date_end))
        date_lables = []
        data_values = []
        spent_values = []
        income_values = []
        values_tmp = {}
        income_values_tmp = {}
        spent_values_tmp = {}
        prev_day_balance = 0

        for op in ops:
            amount = op[0]
            is_income = op[3]
            op_date = '{:%d.%m.%Y}'.format(op[4])
            if op_date not in date_lables:
                date_lables.append(op_date)
                values_tmp[op_date] = prev_day_balance
                income_values_tmp[op_date] = 0
                spent_values_tmp[op_date] = 0
            if is_income:
                values_tmp[op_date] += amount
                income_values_tmp[op_date] += amount
                prev_day_balance = values_tmp[op_date]
            else:
                values_tmp[op_date] -= amount
                spent_values_tmp[op_date] += amount
                prev_day_balance = values_tmp[op_date]
        for one_date_label in date_lables:
            data_values.append(values_tmp[one_date_label])
            income_values.append(income_values_tmp[one_date_label])
            spent_values.append(spent_values_tmp[one_date_label])

        # calculating total max
        max_spent = max(spent_values)
        max_income = max(income_values)
        max_balance = max(data_values)
        total_max = max([max_income, max_spent, max_balance])

        # calculating total min
        min_spent = min(spent_values)
        min_income = max(income_values)
        min_balance = max(data_values)
        total_min = min([min_income, min_spent, min_balance])

        return flask.jsonify({'labels':        date_lables,
                              'values':        data_values,
                              "min":           total_min,
                              "max":           total_max,
                              "spent_values":  spent_values,
                              "income_values": income_values})


@app.route('/', methods=['GET'])
def index():
    # get fin data and render it
    with postgresql.open(conf["postgresql"]) as db:
        today_date = datetime.date.today()
        result_date_start = today_date.replace(day=1)
        result_date_end = result_date_start + relativedelta(months=1)
        locale.setlocale(locale.LC_ALL, ('ru_RU', 'UTF8'))
        opcount, total_income, total_spent = get_operations_in_range(db, result_date_start, result_date_end)
        total_spent = locale.currency(total_spent, grouping=True)
        total_income = locale.currency(total_income, grouping=True)
        locale.setlocale(locale.LC_ALL, '')
    return render_template('index.html', total_income=total_income,
                           total_spent=total_spent, opcount=opcount)


def get_all_operations(db):
    ops = db.query("SELECT bank_operations.amount,bank_operations.\"desc\",bank_operations.cat_name,"
                   "bank_operations.is_income,bank_operations.op_date  FROM   public.bank_operations order by op_date desc;")
    total_income = 0
    total_spent = 0
    for op in ops:
        amount = op[0]
        is_income = op[3]
        if is_income:
            total_income += amount
        else:
            total_spent += amount
    opcount = len(ops)
    return opcount, total_income, total_spent


def get_operations_in_range(db, date_start, date_end):
    ops = db.query("SELECT bank_operations.amount,bank_operations.\"desc\",bank_operations.cat_name,"
                   "bank_operations.is_income,bank_operations.op_date  FROM   public.bank_operations "
                   "where bank_operations.op_date between '{}' and '{}';".format(date_start, date_end))
    total_income = 0
    total_spent = 0
    for op in ops:
        amount = op[0]
        is_income = op[3]
        if is_income:
            total_income += amount
        else:
            total_spent += amount
    opcount = len(ops)
    return opcount, total_income, total_spent


@app.route('/operations', methods=['GET'])
def operations():
    # get fin data and render it
    with postgresql.open(conf["postgresql"]) as db:
        ops = db.query("SELECT bank_operations.amount,bank_operations.\"desc\",bank_operations.cat_name,"
                       "bank_operations.is_income,bank_operations.op_date,bank_operations.id FROM   "
                       "public.bank_operations order by op_date desc;")
        mapped_ops = []

        for op in ops:
            locale.setlocale(locale.LC_ALL, ('ru_RU', 'UTF8'))
            op_amount = locale.currency(op[0], grouping=True)
            locale.setlocale(locale.LC_ALL, '')

            op_object = {
                    "amount":    op_amount,
                    "desc":      op[1],
                    "cat_name":  op[2],
                    "is_income": op[3],
                    "op_date":   op[4],
                    "id":        op[5]
            }
            mapped_ops.append(op_object)
    return render_template('tables.html', operations=mapped_ops)


@app.route("/hiveapp/nutrition", methods=['POST'])
def hive_sync_nutrition():
    """
    Save data provided by device.
    Currently supported Samsung Health format.
    :return:
    """
    if request is not None:
        meals = request.json.get("meals")
        if meals is None:
            return flask.jsonify({"status": "ok"})

        with postgresql.open(conf["postgresql"]) as db:
            day_timestamp = int(request.json.get("dayStart"))
            day = datetime.datetime.fromtimestamp(day_timestamp / 1000.0)

            q = db.prepare("INSERT INTO public.meals(calcium, calories, carbohydate, cholesterol, fat,fiber, iron, "
                           "meal_type, monosaturated_fat, polysaturated_fat, potassium,protein, saturated_fat, "
                           "sodium, sugar, total_fat,trans_fat, vitamin_a, vitamin_c,day) VALUES ($1, $2, $3, $4, "
                           "$5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20);")

            if meals is not None:
                rows = []
                for one_meal in meals:
                    # check if update needed. not insert
                    if day_meal_already_added(db, day, one_meal["meal_type"]):
                        update_day_meal_data(db, day, one_meal)
                    else:
                        x = [x for x in one_meal.values()]
                        args_list_with_day = x.append(day)
                        tuple_args = tuple(args_list_with_day)
                        rows.append(tuple_args)
                q.load_rows(rows)

    print("Request received!")
    return flask.jsonify({"status": "ok"})


@app.route("/hiveapp/steps", methods=['POST'])
def hive_sync_steps():
    """
    Save data provided by device.
    Currently supported Samsung Health format.
    :return:
    """
    if request is not None:
        steps = request.json
        if steps is None:
            return flask.jsonify({"status": "ok"})

        day_timestamp = int(request.json.get("dayStart"))
        day = datetime.datetime.fromtimestamp(day_timestamp / 1000.0)

        with postgresql.open(conf["postgresql"]) as db:
            if day_steps_already_added(db, day):
                update_day_step_data(db, day, steps)
            else:
                insert_day_step_data(db, day, steps)

    print("Request received!")
    return flask.jsonify({"status":"ok"})


@app.route("/hiveapp/sleep", methods=['POST'])
def hive_sync_sleep():
    """
    Save data provided by device.
    Currently supported Samsung Health format.
    :return:
    """
    if request is not None:
        sleep = request.json.get("stages")[0]
        if sleep is None:
            return flask.jsonify({"status": "ok"})

        day_timestamp = int(request.json.get("dayStart"))
        day = datetime.datetime.fromtimestamp(day_timestamp / 1000.0)

        with postgresql.open(conf["postgresql"]) as db:
            if day_sleep_already_added(db, day):
                update_day_sleep_data(db, day, sleep)
            else:
                insert_day_sleep_data(db, day, sleep)

    print("Request received!")
    return flask.jsonify({"status": "ok"})


def insert_day_step_data(db, day, steps):
    q_steps = db.prepare("INSERT INTO public.steps(steps, distance, speed, day) VALUES ($1, $2, $3, $4);")
    q_steps(steps["count"], steps["distance"], steps["speed"], day)


def insert_day_sleep_data(db, day, sleep):
    q = db.prepare("INSERT INTO public.sleep(stage, start_time, end_time, day) VALUES ($1, $2, $3, $4);")
    q(sleep["stage"], sleep["stageStart"], sleep["stageEnd"], day)


@app.route("/hiveapp/exercises", methods=['POST'])
def hive_sync_exercises():
    """
    Save data provided by device.
    Currently supported Samsung Health format.
    :return:
    """
    print("Request received : exercises sync")

    if request is not None:
        exercises = request.json.get("exerciseDtos")

        if exercises is None:
            return flask.jsonify({"status": "ok"})

        with postgresql.open(conf["postgresql"]) as db:
            day_timestamp = int(request.json.get("dayStart"))
            day = datetime.datetime.fromtimestamp(day_timestamp / 1000.0)

            q_row = db.prepare("INSERT INTO public.exercises(calorie, count, type, distance, duration, start_time, "
                               "end_time, day) VALUES ($1, $2, $3, $4, $5, $6, $7, $8);")

            rows_to_load = []
            for exercise in exercises:
                calorie = exercise["calorie"]
                count = exercise["count"]
                exercise_type = exercise["type"]
                distance = exercise["distance"]
                duration = exercise["duration"]
                start_time = exercises["startTime"]
                end_time = exercises["endTime"]
                query = q_row(calorie, count, exercise_type, distance, duration, start_time, end_time, day)
                rows_to_load.append(query)

            if len(rows_to_load) > 0:
                q_row.load_rows(rows_to_load)

    return flask.jsonify({"status": "ok"})


def update_day_meal_data(db_conn, day, meal):
    """
    Update nutrition data from application for the given day.
    Data for that day is overwritten totally.
    We assume that devices have the most recent measurements
    :return:
    """
    q_update = db_conn.prepare("UPDATE public.meals SET calcium=$1, calories=$2, carbohydate=$3, cholesterol=$4, "
                               "fat=$5,fiber=$6, iron=$7, monosaturated_fat=$8, polysaturated_fat=$9, potassium=$10,"
                               "protein=$11, saturated_fat=$12, sodium=$13, sugar=$14, total_fat=$15,trans_fat=$16, "
                               "vitamin_a=$17, vitamin_c=$18 where day=$19 and meal_type=$20")
    q_update(
            meal["calcium"],
            meal["calories"],
            meal["carbohydate"],
            meal["cholesterol"],
            meal["fat"],
            meal["fiber"],
            meal["iron"],
            meal["monosaturated_fat"],
            meal["polysaturated_fat"],
            meal["potassium"],
            meal["protein"],
            meal["saturated_fat"],
            meal["sodium"],
            meal["sugar"],
            meal["total_fat"],
            meal["trans_fat"],
            meal["vitamin_a"],
            meal["vitamin_c"],
            day,
            meal
    )
    #q_update.load_rows([meal_update_query_row])


def update_day_step_data(db_conn, day, step_info):
    """
    Update steps data from application for the given day.
    Data for that day is overwritten totally.
    We assume that devices have the most recent measurements
    :return:
    """
    q_update1 = db_conn.prepare("UPDATE public.steps SET steps=$1, distance=$2, speed=$3 where day=$4;")
    q_update1(
            step_info["count"],
            step_info["distance"],
            step_info["speed"],
            day
    )


def update_day_sleep_data(db_conn, day, sleep_info):
    """
    Update steps data from application for the given day.
    Data for that day is overwritten totally.
    We assume that devices have the most recent measurements
    :return:
    """
    q_update1 = db_conn.prepare("UPDATE public.sleep SET stage=$1, start_time=$2, end_time=$3 where day=$4;")
    q_update1(
            sleep_info["stage"],
            sleep_info["stageStart"],
            sleep_info["stageEnd"],
            day
    )


def day_meal_already_added(db_conn, day, meal):
    """
    Returns True is given day already have meals info
    :param db_conn:
    :param day:
    :param meal:
    :return:
    """
    r = db_conn.query("select day from public.meals where day=$1 and meal_type=$2", day, meal)
    if r is not None and len(r) > 0 is not None:
        return True
    return False


def day_steps_already_added(db_conn, day):
    """
    Returns True if given day already have steps info
    :param db_conn:
    :param day:
    :return:
    """
    r = db_conn.query("select day from public.steps where day=$1", day)
    if r is not None and len(r) > 0 is not None:
        return True
    return False


def day_sleep_already_added(db_conn, day):
    """
    Returns True if given day already have steps info
    :param db_conn:
    :param day:
    :return:
    """
    r = db_conn.query("select day from public.sleep where day=$1", day)
    if r is not None and len(r) > 0 is not None:
        return True
    return False


if __name__ == '__main__':
    app.debug = True
    app.run(host=conf["ip"], threaded=True, port=conf["port"])
