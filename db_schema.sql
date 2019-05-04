CREATE TABLE public.meals
(
  day date NOT NULL,
  id integer NOT NULL DEFAULT nextval('meals_id_seq'::regclass),
  meal_type smallint NOT NULL,
  calories real,
  protein real,
  vitamin_c real,
  vitamin_a real,
  fat real,
  carbohydate real,
  potassium real,
  total_fat real,
  calcium real,
  cholesterol real,
  fiber real,
  iron real,
  monosaturated_fat real,
  polysaturated_fat real,
  saturated_fat real,
  sodium real,
  sugar real,
  trans_fat real,
  CONSTRAINT meals_pk PRIMARY KEY (day, meal_type)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public.meals
  OWNER TO silesta;

-- Index: public.meals_day_calories_idx

-- DROP INDEX public.meals_day_calories_idx;

CREATE INDEX meals_day_calories_idx
  ON public.meals
  USING btree
  (day, calories);
  
  
  
-- Table: public.bank_operations

-- DROP TABLE public.bank_operations;

CREATE TABLE public.bank_operations
(
  amount real NOT NULL DEFAULT 0,
  "desc" text,
  ext_cat_id bigint,
  cat_name text,
  is_income boolean,
  id integer NOT NULL DEFAULT nextval('bank_operations_id_seq'::regclass),
  op_date date,
  source text,
  CONSTRAINT pk PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public.bank_operations
  OWNER TO postgres;
  
  
  
-- Table: public.steps

-- DROP TABLE public.steps;

CREATE TABLE public.steps
(
  day date NOT NULL,
  steps integer,
  distance real,
  speed real,
  CONSTRAINT steps_pk PRIMARY KEY (day)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public.steps
  OWNER TO silesta;
  
  
  