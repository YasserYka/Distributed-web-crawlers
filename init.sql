CREATE TABLE URLs (
    id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    link VARCHAR(255),
    createdAt DATE
);