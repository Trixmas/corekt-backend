# Taiga API Contract

Concerned Module: **Taiga Integration**  
API Version: **V1** (protected with `Authorization: Bearer <token>`)  
Base Path: `/project/{projectId}/versions/{versionId}/taiga`

## Authentication

**All endpoints require JWT authentication** via the `Authorization: Bearer <token>` header. You must first authenticate
through the `/auth` endpoints to obtain a JWT token.

Additionally, endpoints `/projects` and `/us` require a valid Taiga session established through the `/login` endpoint.
The Taiga authentication token is stored server-side after login.

---

## Endpoint: POST - /project/{projectId}/versions/{versionId}/taiga/login

**Description:** Authenticate with Taiga.io and establish a session. This endpoint stores the Taiga authentication token
server-side for subsequent API calls.

**Path Parameters:**

* `projectId` (required): UUID of the project
* `versionId` (required): Integer ID of the version

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

## Endpoint: GET - /project/{projectId}/versions/{versionId}/taiga/projects

**Description:** Retrieve the list of Taiga projects accessible by the authenticated user.

**Path Parameters:**

* `projectId` (required): UUID of the project
* `versionId` (required): Integer ID of the version

**Headers:**

* `Authorization: Bearer <jwt_token>` (required)

**Status Codes:**

* `200` OK
* `401` Unauthorized (invalid/missing JWT token OR not authenticated with Taiga)
* `500` Internal Server Error

**Prerequisites:**

* Valid JWT token in Authorization header
* User must be authenticated via `/project/{projectId}/versions/{versionId}/taiga/login` endpoint

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

## Endpoint: GET - /project/{projectId}/versions/{versionId}/taiga/us

**Description:** Retrieve user stories for a specific Taiga project. This endpoint fetches all user stories associated
with the given project ID, including their subjects and descriptions. The authentication token is removed from the
server after this request completes.

**Path Parameters:**

* `projectId` (required): UUID of the project
* `versionId` (required): Integer ID of the version

**Headers:**

* `Authorization: Bearer <jwt_token>` (required)

**Status Codes:**

* `200` OK
* `400` Bad Request
* `401` Unauthorized (invalid/missing JWT token OR not authenticated with Taiga)
* `404` Not Found (project or version not found)
* `500` Internal Server Error

**Query Parameters:**

* `taigaProjectId` (required): The Taiga project ID to fetch user stories from

**Prerequisites:**

* Valid JWT token in Authorization header
* User must be authenticated via `/project/{projectId}/versions/{versionId}/taiga/login` endpoint
* User must be the owner of the project
* Version must belong to the specified project

**Example Request:**

```
GET /project/123e4567-e89b-12d3-a456-426614174000/versions/1/taiga/us?taigaProjectId=123456
```

**Response payload (`200`):**

```json
{
  "usList": [
    {
      "title": "User Registration",
      "description": "As a user, I want to register an account so that I can access the platform"
    },
    {
      "title": "Dashboard View",
      "description": "As a user, I want to view my dashboard with project statistics"
    },
    {
      "title": "Export Data",
      "description": "As an admin, I want to export user data to CSV format"
    }
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
  "message": "taigaProjectId parameter is required"
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

**Response payload (`404`):**

```json
{
  "usList": null,
  "success": false,
  "message": "Project not found"
}
```

OR

```json
{
  "usList": null,
  "success": false,
  "message": "Version not found"
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

* User stories are returned as objects with `title` and `description` properties
* Each US object has:
    * `title`: The subject/title of the user story
    * `description`: The full description of the user story
* Project data is returned as a map with project names as keys and project IDs as values
* All timestamps and IDs from Taiga are preserved in their original format

### Error Handling

* Connection errors to Taiga API return `500` status codes
* Authentication issues return `401` status codes
* Invalid request data returns `400` status codes
* Resource not found (project/version) returns `404` status codes
* Interrupted requests are handled gracefully with appropriate error messages





