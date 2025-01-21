### Release Notes - Azure AD Extension

## Version 2.0.5

- **Krista Service APIs Java** : 1.0.102
- **Global Catalog Version** : 1.0.53
- **Appliance Release Version** : 3.4.2

## Enhancement Highlights

#### Key Highlights of Version 2.0.5

* **Activate or Deactivate an Account** This request allows admin to activate or deactivate account.
* **Add an Account to Group** (Requires Group.ReadWrite.All permission) Allows admin to add user/account to respective group using group object id and user id or user mail. 
* **Remove an Account From Group** (Requires Group.ReadWrite.All permission) Allows admin to remove user/account from respective group using group object id and user id or user mail.
* **Get List of Groups** Requires Group.ReadWrite.All permission) Used to Fetch List of Group Available.


## Resolved Bugs
NA

## Known Issues

* [KE-1326](https://antbrains.atlassian.net/browse/KE-1326) Extend Refresh Token Validity for Azure AD Extension