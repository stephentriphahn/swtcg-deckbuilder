# SWTCG API Documentation

## Base URL

```
http://localhost:PORT/api/v1
```

## Overview

This API provides endpoints for managing Star Wars Trading Card Game (SWTCG) cards and decks. The API supports card searching, deck creation, and deck validation.

## Interactive Documentation

Swagger UI is available at: `/docs`

## Common Types

### Side

- `L` - Light side
- `D` - Dark side
- `N` - Neutral

### Card Types

- `Character`
- `Space`
- `Ground`
- `Battle`
- `Mission`

### Rarity

- `C` - Common
- `U` - Uncommon
- `R` - Rare
- `P` - Promo

## Health Check

### GET /heartbeat

Health check endpoint to verify the API is running.

**Response**

```
Status: 200 OK
Body: "ok"
```

---

## Cards

### GET /api/v1/cards

List all cards in the system with optional filtering.

**Query Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `search` | string | No | Search cards by name (partial match) |
| `side` | enum | No | Filter by side: `L`, `D`, or `N` |
| `type` | string | No | Filter by card type |
| `set_code` | string | No | Filter by set code (e.g., "AOTC") |
| `limit` | integer | No | Maximum number of results (1-100) |
| `skip` | integer | No | Number of results to skip (for pagination, min: 0) |

**Response**

```json
{
  "cards": [
    {
      "card-id": "7c1e9a4b2d03",
      "name": "Luke Skywalker",
      "type": "Character",
      "side": "L",
      "subtype": "Jedi",
      "set-code": "AOTC",
      "number": 42,
      "rarity": "R",
      "cost": 5,
      "power": 4,
      "health": 3,
      "speed": 2,
      "text": "Card text here",
      "script": "Script/flavor text",
      "usage": "Usage instructions",
      "classification": "Classification",
      "image-file": "/public/setimages/AOTC/image.jpg"
    }
  ]
}
```

**Status Codes**

- `200 OK` - Success

**Example Request**

```bash
curl "http://localhost:3000/api/v1/cards?side=L&type=Character&limit=10"
```

---

### GET /api/v1/cards/:card-id

Get a specific card by its ID.

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `card-id` | string | Yes | Short card id (first 12 hex chars of SHA-1 of the card's image file name) |

**Response**

```json
{
  "card-id": "7c1e9a4b2d03",
  "name": "Luke Skywalker",
  "type": "Character",
  "side": "L",
  "subtype": "Jedi",
  "set-code": "AOTC",
  "number": 42,
  "rarity": "R",
  "cost": 5,
  "power": 4,
  "health": 3,
  "speed": 2,
  "text": "Card text here",
  "script": "Script/flavor text",
  "usage": "Usage instructions",
  "classification": "Classification",
  "image-file": "/public/setimages/AOTC/image.jpg"
}
```

**Status Codes**

- `200 OK` - Card found
- `404 Not Found` - Card does not exist

**Example Request**

```bash
curl "http://localhost:3000/api/v1/cards/7c1e9a4b2d03"
```

---

## Decks

### GET /api/v1/decks

List all decks in the system.

**Response**

```json
[
  {
    "deck-id": "3f2b8c1e-5d4a-4e7b-9c1a-2b6d8e0f4a17",
    "name": "My Light Side Deck",
    "owner": "player123",
    "format": "standard",
    "side": "L"
  }
]
```

**Status Codes**

- `200 OK` - Success

**Example Request**

```bash
curl "http://localhost:3000/api/v1/decks"
```

---

### POST /api/v1/decks

Create a new deck.

**Request Body**

```json
{
  "name": "My Light Side Deck",
  "owner": "player123",
  "format": "standard",
  "side": "L"
}
```

**Field Constraints**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | string | Yes | Minimum length: 1 |
| `owner` | string | Yes | Minimum length: 1 |
| `format` | string | Yes | Minimum length: 1 |
| `side` | enum | Yes | Must be `L` (Light) or `D` (Dark) |

**Response**

```json
{
  "deck-id": "3f2b8c1e-5d4a-4e7b-9c1a-2b6d8e0f4a17",
  "name": "My Light Side Deck",
  "owner": "player123",
  "format": "standard",
  "side": "L"
}
```

**Headers**

- `Location: /api/v1/decks/3f2b8c1e-5d4a-4e7b-9c1a-2b6d8e0f4a17` - URL of the created deck

**Status Codes**

- `201 Created` - Deck successfully created
- `400 Bad Request` - Invalid request body

**Example Request**

```bash
curl -X POST "http://localhost:3000/api/v1/decks" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Light Side Deck",
    "owner": "player123",
    "format": "standard",
    "side": "L"
  }'
```

---

### GET /api/v1/decks/:deck-id

Get a specific deck by ID, including all cards and validation status.

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `deck-id` | string | Yes | The unique deck identifier |

**Response**

```json
{
  "name": "My Light Side Deck",
  "owner": "player123",
  "format": "standard",
  "side": "L",
  "cards": [
    {
      "card-id": "7c1e9a4b2d03",
      "quantity": 2
    },
    {
      "card-id": "b83f60d1e9a7",
      "quantity": 4
    }
  ],
  "validation": {
    "valid?": false,
    "violations": [
      {
        "rule": "deck-size",
        "message": "Deck must contain exactly 60 cards (currently has 6)"
      },
      {
        "rule": "min-characters",
        "message": "Deck must contain at least 12 Character cards (currently has 2)"
      }
    ],
    "warnings": []
  }
}
```

**Validation Rules**

Decks are automatically validated against the following rules:

1. **Deck Size**: Must contain exactly 60 cards
2. **Card Quantity**: No more than 4 copies of any single card
3. **Card Side**: All cards must match the deck's side (Light or Dark) or be Neutral
4. **Minimum Characters**: At least 12 Character cards
5. **Minimum Space**: At least 12 Space cards
6. **Minimum Ground**: At least 12 Ground cards

**Status Codes**

- `200 OK` - Deck found
- `404 Not Found` - Deck does not exist

**Example Request**

```bash
curl "http://localhost:3000/api/v1/decks/3f2b8c1e-5d4a-4e7b-9c1a-2b6d8e0f4a17"
```

---

### DELETE /api/v1/decks/:deck-id

Delete a deck.

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `deck-id` | string | Yes | The unique deck identifier |

**Response**

Empty response body

**Status Codes**

- `204 No Content` - Deck successfully deleted
- `404 Not Found` - Deck does not exist

**Example Request**

```bash
curl -X DELETE "http://localhost:3000/api/v1/decks/3f2b8c1e-5d4a-4e7b-9c1a-2b6d8e0f4a17"
```

---

### PUT /api/v1/decks/:deck-id/cards/:card-id

Add a card to a deck or update its quantity.

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `deck-id` | string | Yes | The unique deck identifier |
| `card-id` | string | Yes | The unique card identifier |

**Request Body**

```json
{
  "quantity": 2
}
```

**Field Constraints**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `card-id` | string | No | Can be provided in path or body |
| `quantity` | integer | Yes | Between 1 and 4 (inclusive) |

**Response**

```json
{
  "deck-id": "3f2b8c1e-5d4a-4e7b-9c1a-2b6d8e0f4a17",
  "card-id": "b83f60d1e9a7",
  "quantity": 2
}
```

**Status Codes**

- `200 OK` - Card added/updated successfully
- `400 Bad Request` - Invalid quantity or card ID
- `404 Not Found` - Deck or card does not exist

**Example Request**

```bash
curl -X PUT "http://localhost:3000/api/v1/decks/3f2b8c1e-5d4a-4e7b-9c1a-2b6d8e0f4a17/cards/b83f60d1e9a7" \
  -H "Content-Type: application/json" \
  -d '{"quantity": 2}'
```

---

### POST /api/v1/decks/:deck-id/cards

Add multiple cards to a deck in a single request.

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `deck-id` | string | Yes | The unique deck identifier |

**Request Body**

```json
[
  {
    "card-id": "7c1e9a4b2d03",
    "quantity": 2
  },
  {
    "card-id": "b83f60d1e9a7",
    "quantity": 4
  },
  {
    "card-id": "04d2c5f7a1e8",
    "quantity": 1
  }
]
```

**Field Constraints**

Each card object must have:

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `card-id` | string | Yes | Valid card ID |
| `quantity` | integer | Yes | Between 1 and 4 (inclusive) |

**Response**

```json
[
  {
    "deck-id": "3f2b8c1e-5d4a-4e7b-9c1a-2b6d8e0f4a17",
    "card-id": "7c1e9a4b2d03",
    "quantity": 2
  },
  {
    "deck-id": "3f2b8c1e-5d4a-4e7b-9c1a-2b6d8e0f4a17",
    "card-id": "b83f60d1e9a7",
    "quantity": 4
  },
  {
    "deck-id": "3f2b8c1e-5d4a-4e7b-9c1a-2b6d8e0f4a17",
    "card-id": "04d2c5f7a1e8",
    "quantity": 1
  }
]
```

**Status Codes**

- `200 OK` - Cards added successfully
- `400 Bad Request` - Invalid request body
- `404 Not Found` - Deck or one of the cards does not exist

**Example Request**

```bash
curl -X POST "http://localhost:3000/api/v1/decks/3f2b8c1e-5d4a-4e7b-9c1a-2b6d8e0f4a17/cards" \
  -H "Content-Type: application/json" \
  -d '[
    {"card-id": "7c1e9a4b2d03", "quantity": 2},
    {"card-id": "b83f60d1e9a7", "quantity": 4}
  ]'
```

---

### DELETE /api/v1/decks/:deck-id/cards/:card-id

Remove a card from a deck.

**Path Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `deck-id` | string | Yes | The unique deck identifier |
| `card-id` | string | Yes | The unique card identifier |

**Response**

Empty response body

**Status Codes**

- `204 No Content` - Card removed successfully
- `404 Not Found` - Deck or card does not exist

**Example Request**

```bash
curl -X DELETE "http://localhost:3000/api/v1/decks/3f2b8c1e-5d4a-4e7b-9c1a-2b6d8e0f4a17/cards/b83f60d1e9a7"
```

---

## Error Responses

All error responses follow this format:

```json
{
  "error": {
    "message": "Error description",
    "type": "error-type",
    "details": {}
  }
}
```

### Common Error Codes

- `400 Bad Request` - Invalid request parameters or body
- `404 Not Found` - Resource does not exist
- `500 Internal Server Error` - Server error

**Example Error Response**

```json
{
  "error": {
    "message": "Card not found",
    "type": "not-found",
    "details": {
      "card-id": "000000000000"
    }
  }
}
```

---

## CORS

CORS is enabled for:
- Origin: `http://localhost:5173`
- Methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`

---

## Validation

### Deck Validation

When retrieving a deck via `GET /api/v1/decks/:deck-id`, the response includes a validation status with the following structure:

```json
{
  "validation": {
    "valid?": boolean,
    "violations": [
      {
        "rule": "rule-identifier",
        "message": "Human-readable error message"
      }
    ],
    "warnings": [
      {
        "rule": "rule-identifier",
        "message": "Human-readable warning message"
      }
    ]
  }
}
```

**Validation Rule Types**

| Rule ID | Severity | Description |
|---------|----------|-------------|
| `deck-size` | Error | Deck must have exactly 60 cards |
| `card-quantity` | Error | No card can exceed quantity of 4 |
| `card-side` | Error | Cards must match deck side or be neutral |
| `min-characters` | Error | Minimum 12 Character cards required |
| `min-space` | Error | Minimum 12 Space cards required |
| `min-ground` | Error | Minimum 12 Ground cards required |

---

## Data Model

### Card Schema

```typescript
{
  "card-id": string,
  "name": string,
  "type": "Character" | "Space" | "Ground" | "Battle" | "Mission",
  "side": "L" | "D" | "N",
  "subtype"?: string | null,
  "set-code": string,
  "number": number | null,
  "rarity": "C" | "U" | "R" | "P",
  "cost"?: number | null,
  "power"?: number | null,
  "health"?: number | null,
  "speed"?: number | null,
  "text"?: string | null,
  "script"?: string | null,
  "usage"?: string | null,
  "classification"?: string | null,
  "image-file"?: string | null
}
```

### Deck Schema

```typescript
{
  "deck-id": string,
  "name": string,
  "owner": string,
  "format": string,
  "side": "L" | "D",
  "cards"?: [
    {
      "card-id": string,
      "quantity": number (1-4)
    }
  ],
  "validation"?: {
    "valid?": boolean,
    "violations": ValidationViolation[],
    "warnings": ValidationViolation[]
  }
}
```

### Validation Violation Schema

```typescript
{
  "rule": keyword,
  "message": string
}
```

---

## Rate Limiting

Currently, no rate limiting is implemented.

---

## Authentication

Currently, no authentication is required. This may change in future versions.

---

## Changelog

### v1 (Current)

- Initial API release
- Card listing and retrieval
- Deck CRUD operations
- Card-to-deck management
- Automatic deck validation
