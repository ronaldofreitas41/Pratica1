# Prática 1: Orçamento por Categoria (Budgets)

> **Duração:** 2 horas. **Stack:** Micronaut 5 + Java 25, PostgreSQL, Kafka, testes com Testcontainers.
> **Ponto de partida:** o projeto com o setup pronto (IntegrationTest + smoke tests passando).
> **Foco desta prática:** o lado **consumer**: agregação, idempotência, regras de tempo e alertas que disparam uma única vez.

---

## Contexto

O Inter quer ajudar os clientes a controlar os gastos do cartão. O cliente define um **limite mensal por categoria** (alimentação, transporte etc.) e recebe um **alerta** quando chega a 80% e a 100% do limite.

O time de cartões já publica todas as compras no Kafka. Seu microsserviço precisa **consumir essas compras, acumular o gasto do mês por categoria e publicar alertas**.

O enunciado é liberado em etapas, como no live code. Só leia a próxima etapa depois de terminar a atual.

---

## Etapa 1: Configurar orçamentos (≈ 25 min)

**`PUT /owners/{ownerId}/budgets/{category}`**
```json
{ "monthlyLimit": 800.00 }
```
Responde `200 OK`:
```json
{ "ownerId": "c0a8...", "category": "FOOD", "monthlyLimit": 800.00, "updatedAt": "2026-11-09T14:00:00Z" }
```

**`GET /owners/{ownerId}/budgets/{category}`** responde `200`, ou `404` em problem+json.

**Regras**
- `category` aceita apenas `FOOD`, `TRANSPORT`, `LEISURE`, `HEALTH` ou `OTHER`. Qualquer outro valor resulta em `400`.
- `monthlyLimit` deve ser maior que 0 e ter no máximo 2 casas decimais.
- Fazer `PUT` de novo na mesma categoria **atualiza** o limite (upsert). Não cria uma segunda linha.
- Todos os erros saem como `application/problem+json`.

---

## Etapa 2: Consumir as compras (≈ 35 min)

O time de cartões publica no tópico **`cards.purchases`**, com a chave `ownerId`:
```json
{
  "purchaseId": "6f1d...uuid",
  "ownerId": "c0a8...uuid",
  "category": "FOOD",
  "amount": 57.90,
  "purchasedAt": "2026-11-30T23:30:00-03:00"
}
```

Crie um `@KafkaListener` que acumule o **gasto do mês** por `ownerId`, categoria e mês.

**Regras**
- O mês é calculado no **fuso de São Paulo** (`America/Sao_Paulo`). Uma compra em `2026-11-30T23:30:00-03:00` pertence a **novembro**, apesar de já ser dezembro em UTC.
- Se a `category` for desconhecida, a compra conta como `OTHER`.
- Compras de categorias **sem orçamento configurado** também são acumuladas. Assim, quem configurar o orçamento no meio do mês já vê o gasto correto.
- **Idempotência:** se o mesmo `purchaseId` chegar duas vezes, ele conta **uma vez só**.
- **Mensagem inválida** (JSON quebrado, `amount` menor ou igual a 0, `ownerId` nulo) **não pode travar o consumer**. Registre em log e siga para a próxima mensagem.

**`GET /owners/{ownerId}/budgets?month=2026-11`** responde `200`:
```json
[
  { "category": "FOOD", "monthlyLimit": 800.00, "spent": 652.40, "percentage": 81.55, "status": "WARNING" },
  { "category": "TRANSPORT", "monthlyLimit": 300.00, "spent": 0.00, "percentage": 0.00, "status": "OK" }
]
```
- O `status` é `OK` abaixo de 80%, `WARNING` entre 80% e menos de 100%, e `EXCEEDED` a partir de 100%.
- Se o parâmetro `month` for omitido, use o mês atual (no fuso de São Paulo).
- `percentage` tem 2 casas decimais, com arredondamento `HALF_UP`.

---

## Etapa 3: Publicar alertas (≈ 35 min)

Quando uma compra fizer o gasto de uma categoria **com orçamento** cruzar um limiar, publique no tópico **`budgets.alerts`**, com a chave `ownerId`:
```json
{
  "alertId": "...",
  "ownerId": "c0a8...",
  "category": "FOOD",
  "month": "2026-11",
  "threshold": 80,
  "spent": 652.40,
  "monthlyLimit": 800.00,
  "occurredAt": "..."
}
```

**Regras**
- Os limiares são **80** e **100**.
- Cada limiar dispara **no máximo uma vez** por dono, categoria e mês, mesmo que outras compras continuem acontecendo acima dele.
- Se uma única compra cruzar os dois limiares ao mesmo tempo (por exemplo, de 70% para 105%), publique **os dois** alertas, nesta ordem: 80 e depois 100.
- Se o limite for alterado no meio do mês, os alertas já enviados naquele mês **não** são reenviados. A próxima compra é avaliada com o limite novo.
- O alerta só pode ser publicado se o gasto foi realmente gravado. Ou seja, nada de publicar antes do commit.

---

## Etapa 4 (bônus, ≈ 25 min): Estornos

O time de cartões também publica no tópico **`cards.refunds`**:
```json
{ "refundId": "...", "purchaseId": "6f1d...", "ownerId": "c0a8...", "amount": 57.90, "refundedAt": "..." }
```

**Regras**
- O estorno **subtrai** do gasto do mês **da compra original**, e não do mês do estorno.
- Um estorno de compra desconhecida é ignorado, com log.
- O mesmo `refundId` duas vezes conta uma vez só.
- O estorno **não** gera alerta nem "desfaz" um alerta já enviado.

---

## Testes esperados (no mínimo)

1. `PUT` válido, `PUT` com categoria inválida (`400`) e `PUT` com limite inválido. Use `@ParameterizedTest`: valor 0, negativo e com 3 casas decimais.
2. Duas chamadas de `PUT` na mesma categoria resultam em uma linha só, com o limite atualizado.
3. Uma compra publicada em `cards.purchases` aparece no `GET` com o `spent` certo (use Awaitility).
4. A mesma compra enviada 2 vezes é somada uma vez só.
5. Uma compra às `23:30 -03:00` do último dia do mês entra no mês certo.
6. Uma mensagem malformada seguida de uma válida: a válida é processada.
7. Cruzar 80% gera exatamente 1 alerta. Compras seguintes entre 80% e 100% não geram outro.
8. Uma compra que cruza 80% e 100% de uma vez gera 2 alertas, na ordem certa.
9. (Bônus) Um estorno reduz o gasto do mês da compra original, e um estorno duplicado conta uma vez só.

---

## Armadilhas

- **Fuso horário:** calcular o mês em UTC (`Instant`) em vez de São Paulo (`ZonedDateTime` / `YearMonth`).
- **"Uma vez só" com `if` em memória:** o controle de "alerta já enviado" e de "compra já processada" precisa estar **no banco**, com `UNIQUE` ou PK, porque o consumer pode ser reiniciado ou ter várias instâncias.
- **Concorrência no acumulado:** fazer `spent = spent + x` lendo para a memória e salvando depois, sem lock, perde atualizações. Pense em `UPDATE ... SET spent = spent + :x` ou em lock otimista com `@Version`.
- **`BigDecimal`:** usar `equals` (`80.0` é diferente de `80.00`) em vez de `compareTo`, e esquecer de arredondar o percentual.
- **Ordem:** a chave da mensagem é o `ownerId`, então as compras de um mesmo cliente chegam em ordem. Saiba explicar por que isso ajuda nos limiares.
- **Teste com `Thread.sleep`:** use `await().untilAsserted(...)`, e `await().during(...)` para provar que **não** chegou um segundo alerta.

## Perguntas que o avaliador pode fazer

- "E se o serviço cair depois de gravar o gasto e antes de publicar o alerta?" (outbox)
- "Como você escalaria isso para 10 instâncias do consumer?" (partições, chave e consumer group)
- "Por que a chave da mensagem é o `ownerId` e não o `purchaseId`?"