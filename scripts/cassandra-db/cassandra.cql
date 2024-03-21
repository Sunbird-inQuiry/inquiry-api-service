CREATE KEYSPACE IF NOT EXISTS dev_hierarchy_store WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': '1'
};

CREATE KEYSPACE IF NOT EXISTS dev_question_store WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': '1'
};

CREATE TABLE IF NOT EXISTS dev_hierarchy_store.questionset_hierarchy (
  identifier text,
  hierarchy text,
  instructions text,
  outcomeDeclaration text,
  PRIMARY KEY (identifier)
);

CREATE TABLE IF NOT EXISTS dev_question_store.question_data (
  identifier text,
  body blob,
  editorState text,
  answer blob,
  solutions text,
  instructions text,
  hints text,
  media text,
  responseDeclaration text,
  interactions text,
  PRIMARY KEY (identifier)
);