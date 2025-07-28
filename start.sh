#!/bin/bash

# Запуск Docker Compose
echo "Запуск контейнеров..."
docker-compose up -d

# Ожидание, пока PostgreSQL не будет доступен
echo "Ожидаем запуск PostgreSQL..."
until docker exec -i postgres pg_isready -U kostya > /dev/null 2>&1; do
  echo "Ожидание... PostgreSQL не доступен"
  sleep 2
done

echo "PostgreSQL доступен!"

# Вывод URL для PgAdmin
echo "PgAdmin доступен по адресу: http://localhost:8000"
echo "Логин: admin@example.com, Пароль: admin"
