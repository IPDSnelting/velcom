CREATE TABLE dimension (
  benchmark      TEXT     NOT NULL,
  metric         TEXT     NOT NULL,
  unit           TEXT     NOT NULL,
  interpretation TEXT     NOT NULL,
  significant    BOOLEAN  NOT NULL,

  PRIMARY KEY (benchmark, metric)
);
