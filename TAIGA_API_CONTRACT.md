# Taiga API Contract

Concerned Module: **Taiga Integration**  
API Version: **V1** (protected with `Authorization: Bearer <token>`)  
Base Path: `/project/taiga`

## Authentication

**All endpoints require JWT authentication** via the `Authorization: Bearer <token>` header. You must first authenticate
through the `/auth` endpoints to obtain a JWT token.

Additionally, endpoints `/projects` and `/us` require a valid Taiga session established through the `/login` endpoint.
The Taiga authentication token is stored server-side after login.

---

## Endpoint: POST - /project/taiga/login

**Description:** Authenticate with Taiga.io and establish a session. This endpoint stores the Taiga authentication token
server-side for subsequent API calls.

**Headers:**

* `Authorization: Bearer <jwt_token>` (required)

**Status Codes:**

* `200` OK
* `400` Bad Request
* `401` Unauthorized (invalid or missing JWT token)
* `500` Internal Server Error

**Request payload:**

```json
{
  "username": "taiga_username",
  "password": "taiga_password"
}
```

**Validation Rules:**

* `username`: Required, must not be blank
* `password`: Required, must not be blank

**Response payload (`200`):**

```json
{
  "success": true,
  "message": "Successfully logged in to Taiga"
}
```

**Response payload (`400`):**

```json
{
  "success": false,
  "message": "Invalid credentials or missing token"
}
```

**Response payload (`500`):**

```json
{
  "success": false,
  "message": "Failed to connect to Taiga: <error_details>"
}
```

---

## Endpoint: GET - /project/taiga/projects

**Description:** Retrieve the list of Taiga projects accessible by the authenticated user.

**Headers:**

* `Authorization: Bearer <jwt_token>` (required)

**Status Codes:**

* `200` OK
* `401` Unauthorized (invalid/missing JWT token OR not authenticated with Taiga)
* `500` Internal Server Error

**Prerequisites:**

* Valid JWT token in Authorization header
* User must be authenticated via `/project/taiga/login` endpoint

**Response payload (`200`):**

```json
{
  "success": true,
  "errorMessage": null,
  "projects": {
    "Project Alpha": "123456",
    "Project Beta": "789012",
    "My Application": "345678"
  }
}
```

**Response payload (`401`):**

```json
{
  "success": false,
  "errorMessage": "Not authenticated with Taiga. Please login first.",
  "projects": null
}
```

**Response payload (`500`):**

```json
{
  "success": false,
  "errorMessage": "Failed to fetch projects from Taiga: <error_details>",
  "projects": null
}
```

---

## Endpoint: GET - /project/taiga/us

**Description:** Retrieve user stories for a specific Taiga project. This endpoint fetches all user stories associated
with the given project ID, including their subjects and descriptions. The authentication token is removed from the
server after this request completes.

**Headers:**

* `Authorization: Bearer <jwt_token>` (required)

**Status Codes:**

* `200` OK
* `400` Bad Request
* `401` Unauthorized (invalid/missing JWT token OR not authenticated with Taiga)
* `500` Internal Server Error

**Query Parameters:**

* `projectId` (required): The Taiga project ID to fetch user stories from

**Prerequisites:**

* Valid JWT token in Authorization header
* User must be authenticated via `/project/taiga/login` endpoint

**Example Request:**

```
GET /project/taiga/us?projectId=123456
```

**Response payload (`200`):**

```json
{
  "usList": [
    "User Registration: As a user, I want to register an account so that I can access the platform",
    "Dashboard View: As a user, I want to view my dashboard with project statistics",
    "Export Data: As an admin, I want to export user data to CSV format"
  ],
  "success": true,
  "message": null
}
```

**Response payload (`400`):**

```json
{
  "usList": null,
  "success": false,
  "message": "projectId parameter is required"
}
```

**Response payload (`401`):**

```json
{
  "usList": null,
  "success": false,
  "message": "Not authenticated with Taiga. Please login first."
}
```

**Response payload (`500`):**

```json
{
  "usList": null,
  "success": false,
  "message": "Failed to fetch user stories from Taiga: <error_details>"
}
```

---

## Notes

### Session Management

* The Taiga authentication token is stored server-side after successful login
* The token is automatically removed after fetching user stories via the `/us` endpoint
* For security, users should re-authenticate before fetching user stories from different projects

### Data Format

* User stories are returned as a concatenated string: `"<subject>: <description>"`
* Project data is returned as a map with project names as keys and project IDs as values
* All timestamps and IDs from Taiga are preserved in their original format

### Error Handling

* Connection errors to Taiga API return `500` status codes
* Authentication issues return `401` status codes
* Invalid request data returns `400` status codes
* Interrupted requests are handled gracefully with appropriate error messages





