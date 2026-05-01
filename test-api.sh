#!/usr/bin/env bash
# ============================================================
# Скрипт тестирования REST API — Order Management BPMN Process
# ============================================================
#
# Обновлён под новую схему:
# - В заказе используются productId + quantity (без productName/price в запросе)
# - Перед заказами создаются товары продавцов
# - Добавлен финальный шаг доставки (DELIVERED)
# - Основные ID берутся из ответов API, а не захардкожены
#
# Использование:
#   chmod +x test-api.sh
#   ./test-api.sh
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080/api}"

# Цвета для вывода
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

STEP=0
FAILED=0
LAST_BODY=""
LAST_CODE=""

print_header() {
    echo ""
    echo -e "${BLUE}============================================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}============================================================${NC}"
}

print_step() {
    STEP=$((STEP + 1))
    echo ""
    echo -e "${YELLOW}--- Шаг $STEP: $1 ---${NC}"
}

pretty_print_json() {
    if command -v jq >/dev/null 2>&1; then
        jq .
    else
        cat
    fi
}

extract_json_field() {
    local json="$1"
    local field="$2"

    if command -v jq >/dev/null 2>&1; then
        printf '%s' "$json" | jq -r "$field"
        return
    fi

    # Fallback без jq: поддержка простых полей вида .id / .courierId
    printf '%s' "$json" | python3 - "$field" <<'PY'
import json
import sys

path = sys.argv[1].strip().lstrip('.')
obj = json.load(sys.stdin)
for key in path.split('.'):
    if not key:
        continue
    if isinstance(obj, dict) and key in obj:
        obj = obj[key]
    else:
        print("null")
        sys.exit(0)

print("null" if obj is None else obj)
PY
}

request() {
    local method="$1"
    local url="$2"
    local expected_codes_regex="$3"
    local payload="${4:-}"

    local response
    if [ -n "$payload" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -d "$payload")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url")
    fi

    LAST_BODY="${response%$'\n'*}"
    LAST_CODE="${response##*$'\n'}"

    if [[ "$LAST_CODE" =~ ^(${expected_codes_regex})$ ]]; then
        echo -e "${GREEN}HTTP $LAST_CODE (ok)${NC}"
    else
        echo -e "${RED}HTTP $LAST_CODE (ожидалось: $expected_codes_regex)${NC}"
        FAILED=$((FAILED + 1))
    fi

    if [ -n "$LAST_BODY" ]; then
        printf '%s' "$LAST_BODY" | pretty_print_json
    fi
}

print_header "ЧАСТЬ 1: СОЗДАНИЕ УЧАСТНИКОВ"

print_step "Создать заказчика #1"
request "POST" "$BASE_URL/customers" "201" '{
  "name": "Иван Иванов",
  "email": "ivan@mail.ru",
  "phone": "+79001234567"
}'
CUSTOMER_1_ID=$(extract_json_field "$LAST_BODY" ".id")

print_step "Создать заказчика #2"
request "POST" "$BASE_URL/customers" "201" '{
  "name": "Мария Петрова",
  "email": "maria@gmail.com",
  "phone": "+79009876543"
}'
CUSTOMER_2_ID=$(extract_json_field "$LAST_BODY" ".id")

print_step "Создать продавца #1"
request "POST" "$BASE_URL/sellers" "201" '{
  "name": "Пиццерия Марио",
  "address": "ул. Ленина, 42"
}'
SELLER_1_ID=$(extract_json_field "$LAST_BODY" ".id")

print_step "Создать продавца #2"
request "POST" "$BASE_URL/sellers" "201" '{
  "name": "Суши-бар Токио",
  "address": "пр. Невский, 100"
}'
SELLER_2_ID=$(extract_json_field "$LAST_BODY" ".id")

print_step "Создать курьера #1"
request "POST" "$BASE_URL/couriers" "201" '{
  "name": "Алексей Доставкин",
  "phone": "+79005551111"
}'
COURIER_1_ID=$(extract_json_field "$LAST_BODY" ".id")

print_step "Создать курьера #2"
request "POST" "$BASE_URL/couriers" "201" '{
  "name": "Дмитрий Быстров",
  "phone": "+79005552222"
}'
COURIER_2_ID=$(extract_json_field "$LAST_BODY" ".id")

print_step "Получить всех заказчиков"
request "GET" "$BASE_URL/customers" "200"

print_step "Получить всех продавцов"
request "GET" "$BASE_URL/sellers" "200"

print_step "Получить всех курьеров"
request "GET" "$BASE_URL/couriers" "200"

print_header "ЧАСТЬ 1.1: СОЗДАНИЕ ТОВАРОВ (для новой схемы заказа)"

print_step "Создать товар #1 у продавца #1"
request "POST" "$BASE_URL/products" "201" "{
  \"sellerId\": $SELLER_1_ID,
  \"name\": \"Пицца Маргарита\",
  \"description\": \"Классическая пицца\",
  \"price\": 599.00
}"
PRODUCT_1_ID=$(extract_json_field "$LAST_BODY" ".id")

print_step "Создать товар #2 у продавца #1"
request "POST" "$BASE_URL/products" "201" "{
  \"sellerId\": $SELLER_1_ID,
  \"name\": \"Кола 0.5л\",
  \"description\": \"Напиток\",
  \"price\": 99.00
}"
PRODUCT_2_ID=$(extract_json_field "$LAST_BODY" ".id")

print_step "Создать товар #1 у продавца #2"
request "POST" "$BASE_URL/products" "201" "{
  \"sellerId\": $SELLER_2_ID,
  \"name\": \"Ролл Филадельфия\",
  \"description\": \"Ролл с лососем\",
  \"price\": 450.00
}"
PRODUCT_3_ID=$(extract_json_field "$LAST_BODY" ".id")

print_step "Создать товар #2 у продавца #2"
request "POST" "$BASE_URL/products" "201" "{
  \"sellerId\": $SELLER_2_ID,
  \"name\": \"Мисо суп\",
  \"description\": \"Традиционный суп\",
  \"price\": 250.00
}"
PRODUCT_4_ID=$(extract_json_field "$LAST_BODY" ".id")

print_step "Получить товары продавца #1"
request "GET" "$BASE_URL/products/seller/$SELLER_1_ID" "200"

print_header "ЧАСТЬ 2: УСПЕШНЫЙ БИЗНЕС-ПРОЦЕСС (по BPMN)"

print_step "BPMN: Заказчик создаёт заказ -> статус IN_PROCESSING"
request "POST" "$BASE_URL/orders" "201" "{
  \"customerId\": $CUSTOMER_1_ID,
  \"sellerId\": $SELLER_1_ID,
  \"items\": [
    {\"productId\": $PRODUCT_1_ID, \"quantity\": 2},
    {\"productId\": $PRODUCT_2_ID, \"quantity\": 2}
  ]
}"
ORDER_1_ID=$(extract_json_field "$LAST_BODY" ".id")

print_step "Проверить уведомления продавца #1 (новый заказ)"
request "GET" "$BASE_URL/notifications/SELLER/$SELLER_1_ID" "200"

print_step "Проверить уведомления заказчика #1 (статус В обработке)"
request "GET" "$BASE_URL/notifications/CUSTOMER/$CUSTOMER_1_ID" "200"

print_step "BPMN: Продавец принимает заказ (canFulfill=true) -> статус COOKING"
request "POST" "$BASE_URL/orders/$ORDER_1_ID/review" "200" '{
  "canFulfill": true
}'

print_step "BPMN: Продавец собирает заказ -> статус ASSEMBLING"
request "POST" "$BASE_URL/orders/$ORDER_1_ID/assemble" "200"

print_step "BPMN: Продавец ищет курьера -> статус SEARCHING_COURIER/AWAITING_COURIER"
request "POST" "$BASE_URL/orders/$ORDER_1_ID/search-courier" "200"
ORDER_1_COURIER_ID=$(extract_json_field "$LAST_BODY" ".courierId")
if [ "$ORDER_1_COURIER_ID" = "null" ] || [ -z "$ORDER_1_COURIER_ID" ]; then
    ORDER_1_COURIER_ID="$COURIER_1_ID"
fi

print_step "Проверить уведомления курьера (новый заказ на доставку)"
request "GET" "$BASE_URL/notifications/COURIER/$ORDER_1_COURIER_ID" "200"

print_step "BPMN: Курьер принимает доставку"
request "POST" "$BASE_URL/orders/$ORDER_1_ID/courier/$ORDER_1_COURIER_ID/accept" "200"

print_step "BPMN: Курьер пришёл в заведение -> статус IN_DELIVERY"
request "POST" "$BASE_URL/orders/$ORDER_1_ID/courier/$ORDER_1_COURIER_ID/arrived" "200"

print_step "BPMN: Курьер доставил заказ клиенту -> статус DELIVERED"
request "POST" "$BASE_URL/orders/$ORDER_1_ID/courier/$ORDER_1_COURIER_ID/deliver" "200"

print_step "Все уведомления заказчика #1 (полная цепочка)"
request "GET" "$BASE_URL/notifications/CUSTOMER/$CUSTOMER_1_ID" "200"

print_header "ЧАСТЬ 3: СЦЕНАРИЙ ОТМЕНЫ ЗАКАЗА ПРОДАВЦОМ"

print_step "Создать заказ #2 (для отмены)"
request "POST" "$BASE_URL/orders" "201" "{
  \"customerId\": $CUSTOMER_2_ID,
  \"sellerId\": $SELLER_2_ID,
  \"items\": [
    {\"productId\": $PRODUCT_3_ID, \"quantity\": 1},
    {\"productId\": $PRODUCT_4_ID, \"quantity\": 1}
  ]
}"
ORDER_2_ID=$(extract_json_field "$LAST_BODY" ".id")

print_step "BPMN: Продавец отклоняет заказ (canFulfill=false) -> статус CANCELLED"
request "POST" "$BASE_URL/orders/$ORDER_2_ID/review" "200" '{
  "canFulfill": false,
  "cancelReason": "Нет нужных ингредиентов"
}'

print_step "Уведомления заказчика #2 (заказ отменён)"
request "GET" "$BASE_URL/notifications/CUSTOMER/$CUSTOMER_2_ID" "200"

print_header "ЧАСТЬ 4: ФИЛЬТРАЦИЯ И ПОИСК"

print_step "Получить все заказы"
request "GET" "$BASE_URL/orders" "200"

print_step "Фильтр по статусу: DELIVERED"
request "GET" "$BASE_URL/orders/status/DELIVERED" "200"

print_step "Фильтр по статусу: CANCELLED"
request "GET" "$BASE_URL/orders/status/CANCELLED" "200"

print_step "Заказы покупателя #1"
request "GET" "$BASE_URL/orders/customer/$CUSTOMER_1_ID" "200"

print_step "Заказы продавца #1"
request "GET" "$BASE_URL/orders/seller/$SELLER_1_ID" "200"

print_step "Заказы курьера (назначенного для заказа #1)"
request "GET" "$BASE_URL/orders/courier/$ORDER_1_COURIER_ID" "200"

print_header "ЧАСТЬ 5: РАБОТА С УВЕДОМЛЕНИЯМИ"

print_step "Непрочитанные уведомления заказчика #1"
request "GET" "$BASE_URL/notifications/CUSTOMER/$CUSTOMER_1_ID/unread" "200"

print_step "Отметить уведомление #1 как прочитанное"
request "POST" "$BASE_URL/notifications/1/read" "200"

print_step "Непрочитанные уведомления заказчика #1 (после прочтения)"
request "GET" "$BASE_URL/notifications/CUSTOMER/$CUSTOMER_1_ID/unread" "200"

print_header "ЧАСТЬ 6: ВАЛИДАЦИЯ (ожидаемые ошибки)"

print_step "Создать заказ без позиций (ошибка валидации 400)"
request "POST" "$BASE_URL/orders" "400" "{
  \"customerId\": $CUSTOMER_1_ID,
  \"sellerId\": $SELLER_1_ID,
  \"items\": []
}"

print_step "Создать заказ с несуществующим покупателем (ошибка 404)"
request "POST" "$BASE_URL/orders" "404" "{
  \"customerId\": 999999,
  \"sellerId\": $SELLER_1_ID,
  \"items\": [{\"productId\": $PRODUCT_1_ID, \"quantity\": 1}]
}"

print_step "Попытка собрать заказ в неправильном статусе (ошибка 409)"
request "POST" "$BASE_URL/orders/$ORDER_1_ID/assemble" "409"

print_step "Получить несуществующий заказ (ошибка 404)"
request "GET" "$BASE_URL/orders/999999" "404"

print_step "Создать заказ с товаром чужого продавца (ошибка 409)"
request "POST" "$BASE_URL/orders" "409" "{
  \"customerId\": $CUSTOMER_1_ID,
  \"sellerId\": $SELLER_1_ID,
  \"items\": [{\"productId\": $PRODUCT_3_ID, \"quantity\": 1}]
}"

print_header "ТЕСТИРОВАНИЕ ЗАВЕРШЕНО"
echo ""
if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}Все шаги выполнены успешно.${NC}"
else
    echo -e "${RED}Есть шаги с неожиданным HTTP-кодом: $FAILED${NC}"
fi

echo "Итого шагов: $STEP"
echo ""

# Полезный код возврата для CI
if [ "$FAILED" -ne 0 ]; then
    exit 1
fi

