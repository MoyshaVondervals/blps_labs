

| POST | `/api/customers` | Создать заказчика |
| GET | `/api/customers` | Получить всех |
| GET | `/api/customers/{id}` | Получить по ID |


| POST | `/api/sellers` | Создать продавца |
| GET | `/api/sellers` | Получить всех |
| GET | `/api/sellers/{id}` | Получить по ID |


| POST | `/api/couriers` | Создать курьера |
| GET | `/api/couriers` | Получить всех |
| GET | `/api/couriers/{id}` | Получить по ID |


| POST | `/api/products` | Создать товар (продавец) |
| PUT | `/api/products/{id}` | Обновить товар |
| DELETE | `/api/products/{id}` | Удалить товар |
| GET | `/api/products/{id}` | Получить товар по ID |
| GET | `/api/products/seller/{sellerId}` | Товары продавца (пагинация: `page`, `size`, `sort`) |
| GET | `/api/products/seller/{sellerId}/available` | Доступные товары продавца |


| POST | `/api/orders` | Создать заказ | Заказчик создаёт заказ |
| POST | `/api/orders/{id}/review` | Проверить заказ | Продавец принимает/отклоняет |
| POST | `/api/orders/{id}/assemble` | Собрать заказ | Продавец собирает |
| POST | `/api/orders/{id}/search-courier` | Искать курьера | Продавец ищет курьера |
| POST | `/api/orders/{id}/courier/{cId}/accept` | Принял доставку | Курьер принимает |
| POST | `/api/orders/{id}/courier/{cId}/arrived` | Пришёл в заведение | Курьер пришёл |
| POST | `/api/orders/{id}/courier/{cId}/deliver` | Доставил заказ | Курьер доставил клиенту |
| GET | `/api/orders/{id}` | — | Получить заказ |
| GET | `/api/orders` | — | Все заказы |
| GET | `/api/orders/status/{status}` | — | По статусу |
| GET | `/api/orders/customer/{id}` | — | Заказы покупателя |
| GET | `/api/orders/seller/{id}` | — | Заказы продавца |
| GET | `/api/orders/courier/{id}` | — | Заказы курьера |


| GET | `/api/notifications/{type}/{id}` | Все уведомления |
| GET | `/api/notifications/{type}/{id}/unread` | Непрочитанные |
| POST | `/api/notifications/{id}/read` | Отметить прочитанным |


| GET | `/api/sse/notifications/CUSTOMER/{id}` | SSE-поток уведомлений покупателя |
| GET | `/api/sse/notifications/SELLER/{id}` | SSE-поток уведомлений продавца |
| GET | `/api/sse/notifications/COURIER/{id}` | SSE-поток уведомлений курьера |


```
# создать участников
curl -X POST http://localhost:8080/api/customers \
  -d '{"name": "Иван Иванов", "email": "ivan@mail.ru", "phone": "+79001234567"}'

curl -X POST http://localhost:8080/api/sellers \
  -d '{"name": "Додопитса", "address": "Микробарберс"}'

curl -X POST http://localhost:8080/api/couriers \
  -d '{"name": "Карапет", "phone": "+79009876543"}'

# 1.5. добавить товары
curl -X POST http://localhost:8080/api/products \
  -d '{"sellerId": 1, "name": "Пицца 4 сыра мазератти", "description": "Пицца 4 сыра мацерари с присадками", "price": 228.00}'

curl -X POST http://localhost:8080/api/products \
  -d '{"sellerId": 1, "name": "Сыр Сухочини", "description": "Итальянский сыр Сухочини, из микробарберса", "price": 1337.00}'

# посмотреть страницу товаров продавца (size=1 для примера)
curl "http://localhost:8080/api/products/seller/1?page=0&size=1&sort=id,asc"

# посмотреть доступные товары
curl http://localhost:8080/api/products/seller/1/available

# создать заказ
curl -X POST http://localhost:8080/api/orders \
  -d '{"customerId": 1, "sellerId": 1, "items": [{"productId": 1, "quantity": 2}]}'

#  продавец принимает заказ
curl -X POST http://localhost:8080/api/orders/1/review \
  -H "Content-Type: application/json" \
  -d '{"canFulfill": true}'

# сборка заказа
curl -X POST http://localhost:8080/api/orders/1/assemble

# поиск курьера
curl -X POST http://localhost:8080/api/orders/1/search-courier

# курьер откликнулся
curl -X POST http://localhost:8080/api/orders/1/courier/1/accept

# 7. курьер забрал
curl -X POST http://localhost:8080/api/orders/1/courier/1/arrived

# 8. курьер доставил заказ
curl -X POST http://localhost:8080/api/orders/1/courier/1/deliver

# проверить уведы
curl http://localhost:8080/api/notifications/CUSTOMER/1

# подписаться
# Покупатель
curl -N http://localhost:8080/api/sse/notifications/CUSTOMER/1
# Продавец
curl -N http://localhost:8080/api/sse/notifications/SELLER/1
# Курьер
curl -N http://localhost:8080/api/sse/notifications/COURIER/1
```
