import flask
import postgresql
from flask import Flask, Response, render_template

app = Flask(__name__)


@app.route('/', methods=['GET'])
def index():
    # get fin data and render it
    with postgresql.open('pq://silesta:123@localhost:5432/silestadb') as db:
        ops = db.query("SELECT bank_operations.amount,bank_operations.\"desc\",bank_operations.cat_name,"
                       "bank_operations.is_income,bank_operations.op_date FROM   public.bank_operations;")

        total_income = 0
        total_spent = 0

        for op in ops:
            amount = op[0]
            desc = op[1]
            cat_name = op[2]
            is_income = op[3]
            op_date = op[4]

            if is_income:
                total_income += amount
            else:
                total_spent += amount
        opcount = len(ops)
    return render_template('index.html', total_income="{0:.2f}".format(total_income),
                           total_spent="{0:.2f}".format(total_spent),
                           opcount=opcount)


@app.route('/operations', methods=['GET'])
def operations():
    # get fin data and render it
    with postgresql.open('pq://silesta:123@localhost:5432/silestadb') as db:
        ops = db.query("SELECT bank_operations.amount,bank_operations.\"desc\",bank_operations.cat_name,"
                       "bank_operations.is_income,bank_operations.op_date FROM   public.bank_operations;")
        mapped_ops = []

        for op in ops:
            op_object = {
                "amount": "{0:.2f} руб.".format(op[0]),
                "desc" : op[1],
                "cat_name": op[2],
                "is_income": op[3],
                "op_date": op[4]
            }
            mapped_ops.append(op_object)
    return render_template('tables.html', operations=mapped_ops)


# @app.route('/get_temp', methods=['GET'])
# def get_temp():
#     db = ValuesDb('db.sqlite')
#     result = db.get_last_temp_value()
#     return flask.jsonify(result)
#
#
# @app.route('/get_last', methods=['GET'])
# def get_last():
#     db = ValuesDb('db.sqlite')
#     data = db.get_last_hundred_temp_value()
#     return flask.jsonify({'labels':data['labels'],'values':data['values']})
#
#
# def save_value(sensor_value):
#     """
#     Saves value to database.
#     :param sensor_value:
#     :return:
#     """
#     pass


if __name__ == '__main__':
    app.debug = True
    app.run(host='0.0.0.0', threaded=True, port=5001)
