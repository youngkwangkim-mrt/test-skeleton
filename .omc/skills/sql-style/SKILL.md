---
name: sql-style
description: SQL style guide with formatting, naming conventions, and query best practices
triggers:
  - sql
  - query
  - ddl
  - dml
  - select
  - create table
argument-hint: ""
---

# SQL style guide

## Overview

This document defines rules for writing readable, maintainable SQL queries following ANSI SQL conventions with project-specific formatting standards.

> **Key Principle**: Use ANSI SQL, lowercase keywords, leading commas, and right-aligned clauses. Optimize for clarity over brevity.

## General principles

* Use **ANSI SQL** as default -- avoid vendor-specific syntax unless necessary
* Use **lowercase** for all SQL keywords and identifiers
* Follow the conventions of the target database (MySQL, Oracle, etc.)
* Write readable, maintainable queries
* Optimize for clarity over brevity

## Formatting rules

### Keyword alignment

Align keywords to create readable structure:

* Main clauses (`select`, `from`, `where`, `group by`, `order by`, `limit`) right-aligned
* `having` left-aligned at column 0
* `join` indented under `from`
* `on` indented under `join`

```sql
select u.id
     , u.name
     , u.email
     , o.order_date
     , o.total_amount
  from users u
       inner join orders o
                  on u.id = o.user_id
 where u.status = 'active'
   and o.order_date >= '2024-01-01'
 group by u.id
        , u.name
        , u.email
        , o.order_date
        , o.total_amount
having count(*) > 1
 order by o.order_date desc
 limit 10
```

### Comma placement

Place commas at the **beginning** of lines (leading commas):

```sql
-- Good: Leading commas
select id
     , name
     , email
     , created_at
  from users

-- Bad: Trailing commas
select id,
       name,
       email,
       created_at
  from users
```

Benefits of leading commas:

* Easy to comment out columns
* Clear visual alignment
* Simpler version control diffs

### Indentation

* Use consistent indentation
* Align related elements vertically
* `join` indented 7 spaces from `from`
* `on` indented to align after `join` clause

```sql
select p.product_id
     , p.product_name
     , c.category_name
     , sum(oi.quantity) as total_sold
  from products p
       join categories c
            on p.category_id = c.category_id
       join order_items oi
            on p.product_id = oi.product_id
 where p.is_active = true
   and c.category_name in ('Electronics', 'Clothing')
 group by p.product_id
        , p.product_name
        , c.category_name
```

## Naming conventions

### Tables

* Use **snake_case** for table names
* Use **plural** nouns for table names
* Use meaningful, descriptive names

```sql
-- Good
users
order_items
product_categories

-- Bad
User
OrderItem
tbl_products
```

### Columns

* Use **snake_case** for column names
* Use consistent prefixes for related columns
* Avoid abbreviations unless widely understood
* **Do NOT use database `ENUM` type** for code/status columns — always use `varchar` (see below)

```sql
-- Good
user_id
created_at
is_active
total_amount

-- Bad
userId
createdAt
isActive
tot_amt
```

### Code/status columns

> **IMPORTANT**: Do not use the database `ENUM` type for code or status columns in DDL. Always use `varchar` instead. Database `ENUM` types require schema migrations (ALTER TABLE) to add or remove values, whereas `varchar` columns allow application-level changes without DDL modifications.

```sql
-- Good: varchar for code/status columns
create table orders (
    id           bigint
  , status       varchar(20) not null   -- managed by application enum
  , payment_type varchar(20) not null   -- managed by application enum
  , constraint pk_orders primary key (id)
);

-- Bad: database ENUM type — requires ALTER TABLE to add new values
create table orders (
    id           bigint
  , status       enum('PENDING', 'PAID', 'SHIPPED') not null
  , payment_type enum('CREDIT_CARD', 'BANK_TRANSFER') not null
  , constraint pk_orders primary key (id)
);
```

### Primary and foreign keys

* Primary key: `id` or `{table_name}_id`
* Foreign key column naming: `{referenced_table}_id`
* **Do NOT add foreign key constraints by default** -- only when explicitly requested

### Constraint naming conventions

Use the following naming patterns for constraints:

| Type | Pattern | Example |
|------|---------|---------|
| Primary Key | `pk_{table_name}` | `pk_orders` |
| Unique Key | `uk_{table_name}_01`, `_02`, ... | `uk_users_01` |
| Foreign Key | `fk_{table_name}_01`, `_02`, ... | `fk_orders_01` |
| Index | `idx_{table_name}_01`, `_02`, ... | `idx_orders_01` |
| Sequence | `seq_{table_name}_01`, `_02`, ... | `seq_orders_01` |
| Check | `ck_{table_name}_01`, `_02`, ... | `ck_orders_01` |

```sql
-- Default: No FK constraint, no index
create table orders (
    id           bigint
  , user_id      bigint not null
  , total_amount decimal(10, 2)
  , created_at   timestamp default current_timestamp
  , constraint pk_orders primary key (id)
  , constraint uk_orders_01 unique (user_id, created_at)
);

-- Suggested indexes:
-- create index idx_orders_01 on orders(user_id);
-- create index idx_orders_02 on orders(created_at);

-- Only when explicitly requested: With FK constraint
create table orders (
    id           bigint
  , user_id      bigint not null
  , total_amount decimal(10, 2)
  , created_at   timestamp default current_timestamp
  , constraint pk_orders primary key (id)
  , constraint fk_orders_01
      foreign key (user_id) references users(id)
);
```

## Query best practices

### SELECT statements

* Avoid `select *` in production code
* Always specify column names explicitly
* Use table aliases for multi-table queries

```sql
-- Good
select u.id
     , u.name
     , u.email
  from users u
 where u.status = 'active'

-- Bad
select *
  from users
 where status = 'active'
```

### JOIN clauses

* Always use explicit JOIN syntax (not implicit comma joins)
* Place join conditions on separate lines with `on`

```sql
-- Good: Explicit JOIN
select u.name
     , o.order_date
  from users u
       inner join orders o
                  on u.id = o.user_id

-- Bad: Implicit join
select u.name
     , o.order_date
  from users u
     , orders o
 where u.id = o.user_id
```

### WHERE clauses

* Place each condition on a new line
* Align `and` / `or` operators

```sql
select id
     , name
  from users
 where status = 'active'
   and created_at >= '2024-01-01'
   and (role = 'admin'
        or role = 'manager')
```

### Subqueries

* Indent subqueries consistently
* Use CTEs (Common Table Expressions) for complex queries

```sql
-- Using CTE (preferred for complex queries)
with active_users as (
    select id
         , name
      from users
     where status = 'active'
)
, recent_orders as (
    select user_id
         , count(*) as order_count
      from orders
     where order_date >= current_date - interval '30' day
     group by user_id
)
select au.name
     , ro.order_count
  from active_users au
       join recent_orders ro
            on au.id = ro.user_id
```

### INSERT statements

```sql
insert into users (
    name
  , email
  , status
  , created_at
) values (
    'John Doe'
  , 'john@example.com'
  , 'active'
  , current_timestamp
);
```

### UPDATE statements

```sql
update users
   set name = 'Jane Doe'
     , email = 'jane@example.com'
     , updated_at = current_timestamp
 where id = 1;
```

### DELETE statements

```sql
delete from orders
 where status = 'cancelled'
   and created_at < current_date - interval '1' year;
```

## Performance best practices

### Indexing

* **Do NOT create indexes by default** -- only suggest if needed
* Always suggest indexes when beneficial for query performance
* Consider indexes for frequently queried columns and foreign key columns

> **Tip**: When writing DDL, suggest indexes as comments instead of creating them:

```sql
create table orders (
    id           bigint
  , user_id      bigint not null
  , order_date   date
  , constraint pk_orders primary key (id)
);

-- Suggested indexes:
-- create index idx_orders_01 on orders(user_id);
-- create index idx_orders_02 on orders(order_date);
```

### Query optimization

* Use `exists` instead of `in` for subqueries when possible
* Avoid functions on indexed columns in WHERE clauses
* Use `limit` for large result sets

```sql
-- Good: EXISTS
select u.name
  from users u
 where exists (
    select 1
      from orders o
     where o.user_id = u.id
       and o.status = 'completed'
)

-- Avoid: Function on indexed column
-- Bad
select * from users where year(created_at) = 2024
-- Good
select * from users where created_at >= '2024-01-01' and created_at < '2025-01-01'
```

## Database-specific conventions

### MySQL

* Use backticks for reserved words if necessary
* Use `auto_increment` for primary keys
* Use `datetime` or `timestamp` for date/time columns

### PostgreSQL

* Use `serial` or `bigserial` for auto-increment
* Use `timestamptz` for timezone-aware timestamps
* Leverage PostgreSQL-specific features (arrays, jsonb, etc.)

### Oracle

* Use sequences for primary key generation
* Use `nvl` or `coalesce` for null handling
* Follow Oracle naming length limits (30 characters)

## Comments

* Use comments to explain complex logic
* Document non-obvious business rules

```sql
-- Get users who have placed orders in the last 30 days
-- but have not logged in during the same period (potential churn risk)
with recent_orders as (
    select distinct user_id
      from orders
     where order_date >= current_date - interval '30' day
)
select u.id
     , u.name
     , u.email
  from users u
       join recent_orders ro
            on u.id = ro.user_id
 where u.last_login_at < current_date - interval '30' day
```
