INSERT INTO countries (name, iso_code)
VALUES ('México', 'MEX')
ON CONFLICT (iso_code) DO NOTHING;