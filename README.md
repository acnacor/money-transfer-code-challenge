# Account Transfer API

## Project Summary
This project provides a Java 21 **RESTful API for transferring money between accounts** in different currencies. It handles **transfer fees**, **foreign exchange (FX) conversions**, and **concurrent transactions**, ensuring accurate balance updates and transaction logging.

The system is implemented using **Spring Boot**, **JPA/Hibernate**, and **H2 in-memory database** for integration testing. All integration tests are self-contained and reproducible with `mvn clean install`.

---

## Available APIs

| Method | Endpoint | Description | Request Body | Response |
|--------|---------|-------------|--------------|----------|
| POST   | /api/transfers | Transfer money from one account to another | TransferRequest { fromAccountId, toAccountId, amount } | TransferResponse { status, message } |
| GET    | /api/accounts/{id} | Get account details including current balance | N/A | Account { id, name, balance, currency } |
| GET    | /api/accounts | List all accounts | N/A | List<Account> |

---

## Preseeded Data
All preloaded accounts and FX rates are included in the project files under resources

- `schema.sql` – Defines tables and constraints.
- `data.sql` – Seeds accounts and FX rates for testing.

---

## Assumptions

- A **1% transfer fee** is applied to the sender's account on every transfer.
- Transfers **require a valid FX rate** for currency conversion; otherwise an error is thrown.
- **Bob’s currency is AUD** (not JPY) — corrected from the original problem statement.
- Transfers **cannot exceed the sender’s balance** including fees. Insufficient funds result in an error.
- Each account has a **single base currency**. Cross currency transfers must use FX conversion.

---

## Concurrency Handling

Multiple simultaneous transfers are safely handled using **database transactions and row-level locking**:

- **Transactional Operations:** Each transfer runs inside a Spring `@Transactional` method to ensure atomicity.
- **Row Level Locking:** The sender’s and receiver’s account rows are locked during the transaction to prevent concurrent modifications that could cause overdrafts or inconsistent balances.

---

## How to Run the Project

1. Extract the ZIP file to a folder on your machine.
2. Open a terminal and navigate to the extracted project folder.

OR

1. Clone the Project Repository

   git clone https://github.com/acnacor/money-transfer-code-challenge.git

2. cd money-transfer-code-challenge

------------------------------------------------

3. Build the project using Maven:

   mvn clean install

4. Run the Spring Boot application:

   mvn spring-boot:run

5. Access the API at http://localhost:8080 using Postman, curl, or any REST client.

---

## Example API Calls

**Transfer 50 USD from Alice to Bob**
```
curl -X POST http://localhost:8080/api/transfers -H "Content-Type: application/json" -d '{"fromAccountId":"11111111-1111-1111-1111-111111111111","toAccountId":"22222222-2222-2222-2222-222222222222","amount":50.00}'
```
**Check Alice’s account balance**

```
curl http://localhost:8080/api/accounts/11111111-1111-1111-1111-111111111111
```

**Check Bob’s account balance**
```
curl http://localhost:8080/api/accounts/22222222-2222-2222-2222-222222222222
```

**List all accounts**
```
curl http://localhost:8080/api/accounts
```

---

## Testing

Integration tests are located in:  
```
src/test/java/com/example/account_transfer_api/integration/TransferAPIIT.java
```

Tests cover all scenarios described in the problem statement, including:

- Simple transfers (USD → AUD, AUD → USD)
- Transfers with fees applied
- Transfers exceeding available balance
- Recurring transfers
- Concurrent transfers

