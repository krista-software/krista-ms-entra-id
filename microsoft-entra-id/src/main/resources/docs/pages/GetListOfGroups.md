# Get List of Groups

## Overview

The **Get List of Groups** catalog request retrieves all security and distribution groups from Microsoft Entra ID.

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Account Manager |
| **Type** | CHANGE_SYSTEM |
| **Retry Support** | No |

## Input Parameters

None

## Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| Groups | List | Array of group objects with id and displayName |

## Usage Examples

```
(No input parameters required)

Output:
Groups: [
  {"id": "abc123...", "displayName": "Sales Team"},
  {"id": "def456...", "displayName": "Engineering"}
]
```

## Business Rules

1. **All Groups**: Returns all groups in the tenant
2. **Security and Distribution**: Includes both types
3. **No Filtering**: Returns complete list

## Related Catalog Requests

- [Add an Account to Group](pages/AddAnAccountToGroup.md)
- [Remove an Account From Group](pages/RemoveAnAccountFromGroup.md)

## See Also

- [Microsoft Graph List Groups API](https://learn.microsoft.com/en-us/graph/api/group-list)
