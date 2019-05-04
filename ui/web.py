import datetime

import flask
import postgresql
import json
import locale
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

        return flask.jsonify({'labels': date_lables,
                              'values': data_values,
                              "min": total_min,
                              "max": total_max,
                              "spent_values": spent_values,
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
                "amount": op_amount,
                "desc": op[1],
                "cat_name": op[2],
                "is_income": op[3],
                "op_date": op[4],
                "id": op[5]
            }
            mapped_ops.append(op_object)
    return render_template('tables.html', operations=mapped_ops)


@app.route("/hiveapp", methods=['POST'])
def hive_sync():
    if request is not None:
        data = request.json.get("data")
        meals = data["meals"]
        steps = data["steps"]
        heart_rate = data["heart_rate"]
        exercise = data["exercise"]
        water = data["water"]
        temperature = data["temperature"]
        with postgresql.open(conf["postgresql"]) as db:
            if meals is not None:
                q = "INSERT INTO public.meals(day, id, meal_type, calories, protein, vitamin_c, vitamin_a," \ 
            "fat, carbohydate, potassium, total_fat, calcium, cholesterol," \ 
            "fiber, iron, monosaturated_fat, polysaturated_fat, saturated_fat," \ 
            "sodium, sugar, trans_fat)"\
    "VALUES (?, ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?,
            ?, ?, ?);
"


            ops = db.query("SELECT bank_operations.amount,bank_operations.\"desc\",bank_operations.cat_name,"
                           "bank_operations.is_income,bank_operations.op_date,bank_operations.id FROM   "
                           "public.bank_operations order by op_date desc;")


    print("Request received!")
    return "ok"


if __name__ == '__main__':
    app.debug = True
    app.run(host=conf["ip"], threaded=True, port=conf["port"])
