# Aurora Authentication and Authorization PoC
 
This project presents a proof of concept on how to perform authentication and fine-grained authorization on aurora stack.
It is a working prototype of the backend side of a secure multi-tenant application.
 
The main idea of this structure is to have authentication at the entry of the API and fine-grained authorization on each microservice. 
 
## Components
 
* API Gateway (ambassador)
* Keycloak
* Authentication Service
* Working User/Role/ApiKey Resource Service (embedded on Authentication Service for simplicity)
* Mock Satellite-Management Resource Service (embedded on Authentication Service for simplicity)
 
Both ambassador and keycloak are directly accessible by the internet while the authentication and resource services are behind the gateway.
  
## Architecture and Authentication/Authorization Flow

The authentication/authorization flow is explained in the image bellow where the items in green are executed periodically (first request and cache miss)
and the red items represent the bad path, where validation fails.

![Authentication/Authorization Flow](images/client-flow.png?raw=true)

## How to Run

### Pre-Requisites
* docker
* kubernetes
* kubectl
* sed

### Deploying

The deploy.sh script builds and deploys the whole infrastructure automatically.

1. Clone project
2. `cd project`
3. `./deploy.sh`
4. Keycloak admin console is available at `http://localhost:8080`
4. API is available at `http://localhost/`

Notes:
* Deploy might take a while (needs to download and build various components)
* Make sure to run the script from the project source, so it can build and mount volumes correctly
* **Be aware of your kubernetes context, so you deploy to an empty or development cluster**

### Using and Testing

The deploy script creates one organization called `waterdog`, with two roles (`Administrator and `BasicUser`).
It also creates two users, one for each role:
* Username: miguel, Password: miguel, Role: Administrator
* Username: bruno, Password: bruno, Role: BasicUser

Since this is a backend application there is no frontend so, all interactions are performed via http calls (curl, postman, etc)

#### Login

```
curl --location --request POST 'http://localhost:8080/auth/realms/waterdog/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'username=miguel' \
--data-urlencode 'password=miguel' \
--data-urlencode 'client_id=frontend'
```

Collect the `access_token` in the response to access the resource server.

#### Resource Server

For example:

```
curl --location --request GET 'http://localhost/satellite/1/system/2' \
--header 'Authorization: Bearer eyJhbGciOiJSUz...'
```

To test authentication and authorization use the mock satellite-management service since is the only mocked.
Other resource services are working.

### Api Documentation

The full openapi3.0 is not available yet, so the next sections explain the api available for each service:

**Note:** Required scopes for each method are not listed bellow. For now, check the source code or use Administrator user that has full privileges.

#### User Management

* List users `GET /user`
* Create user (via form) `POST /user`
* Setup a user by invite (send an email to target user and he setups is account (first name, last name, username, password)) `POST /user/invite` *
* View user `GET /user/{id}`
* Edit user `PUT /user/{id}`
* Delete user `DELETE /user/{id}`
* Force reset password (new password needed on next login) `PUT /user/{id}/force-resetpwd` **
* Reset password via email `PUT /user/{id}/resetpwd` *
* Force user to setup totp (2FA setup needed on next login) `PUT /user/{id}/force-otp` **
* Configure 2FA via via emai `PUT /user/{id}/force-otp` *
* Assign a role to a user `PUT /user/{id}/role`
* Unassign a role to a user `DELETE /user/{id}/role`
* Disable an user `PUT /user/{id}/disable`
* Enable an user `PUT /user/{id}/enable`

\*: Will not work until smtp server is configured in keycloak

\*\*: Will not work because there is no login screen in this example (although it was tested and is working correctly with login frontend)

#### Session Management

* List sessions of an user `GET /user/{id}/session`
* Revoke a single user session `DELETE /user/{id}/session/{sessionId}`
* Revoke all user sessions (Force logout) `DELETE /user/{id}/session`

#### Role Management

* List Roles `GET /role`
* Create Role `POST /role`
* View Role `GET /role/{id}`
* Edit Role `PUT /role/{id}`
* Delete Role `DELETE /role/{id}`

#### Satellite Management Service (Mock Service)

This mock resource server is for scope authorization demonstration purposes. It will only return 200, 401, 403 status depending on authorization.

* `GET /satellite`
* `POST /satellite`
* `PUT /satellite{satelliteId}`
* `GET /satellite/{satelliteId}`
* `DELETE /satellite/{satelliteId}`
* `GET /satellite{satelliteId}/system`
* `POST /satellite{satelliteId}/system`
* `GET /satellite/{satelliteId}/system/{systemId}`
* `PUT /satellite{satelliteId}/system/{systemId}`

## Deploying new organization

### Automatically
From project root run `./deploy-new-organisation.sh` and follow the script.

Note: Create user by invite is not yet available in the script. Check [TODO](#todo) section.

### Manually
#### Required Steps
1. On the top left of the page, hover on current realm name and click add realm.
2. Select new organization name and create the realm.
3. On the left menu select Import.
4. On "select file", select default template provided [here](k8/resources/realm-default-template.json).
5. Change the "If a resource exists" to "Skip".
6. Make sure all options are enabled.
7. Navigate to Clients > frontend.
8. Change Root Url, Valid Redirect Urls and Web Origins to match new organization urls.
9. Navigate to Clients > backend > Credentials.
10. Regenerate Secret.
11. Download configuration files.
    11.1. Navigate to Clients > backend > Installation > Keycloak OIDC JSON.
    11.2. Download and place it int backend realm folder.
    11.3. Navigate to Clients > frontend > Installation > Keycloak OIDC JSON.
    11.4. Download and place it int frontend realm folder.


#### Required Steps - Create an administrator user
There are two options here: Create a complete user via keycloak or setup a user invite so the user can setup his own profile.
(Email providers configurations required)

**Create complete user via keycloak:**
1. On the left menu, navigate to Users.
2. Click add user.
3. Setup username, email, first name, last name, check user enabled.
4. If you trust the user check email verified to on. If not, leave it off and on "Required User Actions" select verify email (user will be prompt to verify his email on first login).
5. Save.
6. On the new user menu navigate to Credentials and set a new password. Leave temporary if you want for the new user to choose a new one on first login.
7. On the new user menu navigate to Role Mappings.
8. On Client Roles type backend, select Administrator and "Add sellected >>".

**Create user via invite:**
1. On the left menu, navigate to Users.
2. Click add user.
3. Setup placeholder username (eg: first part of email) and email of the user to be invited. (Username and email are the only required configurations. Username can be edited by the user on first login).
4. On "Required User Actions" select "Update Password" and "Update profile".
5. Save.
6. On the new user menu navigate to Role Mappings.
7. On Client Roles type backend, select Administrator and "Add sellected >>".
8. Navigate to Credentials.
9. On "Credential Reset" section add the "Verify Email" and send the email.
10. User will receive an email with a link so it can verify its email. It will also be prompt to update his own profile, username, first and last name and to choose a password.

**Note:** To test with Direct Access Grant Flow has to be enabled. To enable this, navigate to keycloak page, the correct realm, and on the frontend client enable the Direct Access Grant Flow.
It should not be enabled in production.

#### Optional Steps

1. If there is authentication on email provider, the password must be set again since the default realm does not store passwords. On Realm Settings > Email set it up again.
2. If you have a custom theme for the organization configure it on Realm Settings > Themes.
3. If you want to configure different session timeouts and different jwt token expiration set it up on Realm Settings > Tokens.

## Under the Hood

### Project Structure

```
root
│   README.md
│   deploy.sh
|   k8/ (folder containing kubernetes files) 
│
└───src/main
│   |
|   └───resources
|   |   │   application.conf
│   │
│   └───koltin/test/ktortemplate
│       │   authservice (represents authentication service)
|       |   |
|       |   └───httphandler
|       |   └───model
|       |   └───service
│       │   common (represents common stuff, configurations, models, utils, handlers)
|       |   |
|       |   └───httphandler
|       |   └───conf
|       |   └───model
|       |   └───utils
│       │   usermanagement (represents user management service, users, roles and apikeys)
|       |   |
|       |   └───httphandler
|       |   └───model
|       |   └───service
│       │   satmanagement (represents mock satellite management service)
|       |   |
|       |   └───httphandler
```

#### Entitlements

The entitlements are a JSON file that specify which resources a certain role has access to.
It is stored in keycloak alongside a role, as its attribute.
They are divided by services and within each service, by parent resources. Since the same system can be included in different
satellites, there is a need to define a parent resource and a child resource. The parent resource is the bigger context on which
the authorization is going to be made. The permissions for a parent resource are a set of actions on a set of child resources.

We can say that a certain action can be performed to a certain resource within a certain context. For example,
the action `EditSystem` can be performed to the system with id 3 within the context of the satellite 1.

Bellow are some examples of the first entitlement JSON structure.

Administrator entitlement json:
```
[
  {
    "service": "satellite-management",
    "resourcePermissions": [
      {
        "parentResource": "*",
        "permissions": [
          {
            "actions": [
              "*"
            ],
            "resources": [
              "*"
            ]
          }
        ]
      }
    ]
  },
  {
    "service": "user-management",
    "resourcePermissions": [
      {
        "parentResource": "*",
        "permissions": [
          {
            "actions": [
              "*"
            ],
            "resources": [
              "*"
            ]
          }
        ]
      }
    ]
  }
]
```

BasicUser entitlement json:
```
[
  {
    "service": "satellite-management",
    "resourcePermissions": [
      {
        "parentResource": "satellite",
        "permissions": [
          {
            "actions": [
              "listSatellite",
              "createSatellite"
            ],
            "resources": [
              "*"
            ]
          }
        ]
      },
      {
        "parentResource": "satellite:1",
        "permissions": [
          {
            "actions": [
              "viewSatellite",
              "createSystem"
            ],
            "resources": [
              "*"
            ]
          },
          {
            "actions": [
              "viewSystem"
            ],
            "resources": [
              "system:1"
            ]
          }
        ]
      },
      {
        "parentResource": "satellite:2",
        "permissions": [
          {
            "actions": [
              "*"
            ],
            "resources": [
              "*"
            ]
          }
        ]
      }
    ]
  }
]
```

### AuthZ Token

The AuthZ token a Base64 encoded token that is passed to endpoints has a header named `X-Authz-Token`.
This token is a JSON representation of the principal trying to access to system. It is built by the auth-service after 
authenticating the request against keycloak server, and it should identify the principal trying to access the system and 
provide information about him, for authorization and audit purposes.

It has the structure defined bellow:

```
{
  "userId": "123456789",
  "userName": "test-username",
  "organizationId": "test-organization",
  "role": {
    "name": "Administrator",
    "entitlements": [ ... ]
}
```

Due to the Header Authentication Provider explained bellow, all endpoints can access the Principal by calling 
`call.principal(): AuthenticatedPrincipal`.

### Enforcing Authorization

Each endpoint on each resource server is responsible to enforce its own authorization.

A high level wrapper function was implemented to check if a user contains the entitlements required to access the endpoint.
Each endpoint must point out the required actions and resources just like the example bellow:

```
fineGrainedAuthorization(
            "satellite-management",
            "satellite:$satelliteId",
            "viewSystem",
            "system:$systemId"
) { authorizedUser ->
    body(...)
}
```

This allows for some computation being performed before enforcing authorization.
For example, in a request to `/telecommand/123` check the target system of telecommand 123 and enforce authorization to the system.

This high order function could be stored in a shared repository, so it can be access by any service as a dependency.

### Header Authentication Provider

Intra cluster authentication works as it is currently working. Endpoints are inside an "authenticate KTOR function" that checks if the AuthZToken header is present.

```
install(Authentication) {
    auroraHeaderAuthentication("auroraHeaderAuthentication")
}

install(Routing) {
    authenticate("auroraHeaderAuthentication") {
        resourcesRoutes()
    }
}
```

The AuthZ Token header is denied by the ambassador by default if passed from outside the cluster and injected by the auth-service for the requests intra-cluster
which means that a malicious token cannot be injected from the outside. If the header token is not present then something went wrong and the request is denied in the service.
If present, the request goes through, and a Principal class is set to the request context, meaning that the principal accessing the API can be used in each endpoint. 

```
class AuthenticatedPrincipal(val userId: String, val userName: String, val organizationId: String, val role: UserRole) : Principal
```

The `auroraHeaderAuthentication` implementation is provider [here](src/main/kotlin/test/ktortemplate/common/utils/HeaderAuthenticationProvider.kt), but it should be moved to a shared repository, so it
can be accessed by all services as a dependency (as it is currently implemented).

## Infrastructure Caveats and Possible Solutions

The first caveat to point is that the role attributes column in the keycloak has to be changed from a `VARCHAR(255)` to a type `TEXT`.
This has a simple solution since we control the database, just changed directly with a psql query.

Then, the entitlements json can become quite large if there is a very detailed fine-grained authorization for that role. 
A possible solution is to group as many resources as possible with an `*` when a certain role as access to all the resource/sub-resource.
This should help the json size.

The size of the AuthZ Token Header can also be reduced by the authentication service only sending the entitlements required by the service.
Example, if we know the request is accessing the satellite-management-service, filter the entiletments and only send the ones of the satellite-management-service.
It can go even further by knowing which parent resource is the request accessing and filtering the entitlements by service and be parent resource.
It needs testing...

Performance issues with big AuthZ tokens flying around the cluster. A possible solution is to cache the entitlements on the auth-service instead of requesting them every request.
A redis server can be used as a side container to the auth-service to cache them.
if it is not enough, the AuthZ token can be avoided by having all services accessing the keycloak and asking for the entitlements themselves,
or instead of accessing the keycloak, maybe access a shared cache (Redis Server) managed by the auth-service.

Cache invalidation can be a big problem. Caching signing keys and role entitlements. Should have a clear invalidation strategy
so they can be refreshed and invalidated when expired (Redis has built-in expiration to keys) or even at request if needed.

Signing keys cache invalidation could be avoided by introspecting the authentication token in keycloak with a request (like is currently working in staging/production)
and the same goes for entitlements, by requesting to keycloak at every client request, although not caching can decrease performance.

Performance testing needs to be done.

## TODOs

### Setup Keycloak Default Realm Template

The keycloak default setup needs to be tailored to production needs. Example is the configuration of the smtp server,
the token and sso session timeouts, default roles, password policies (1 number, 1 special character etc...) and event logging etc...

### Extend Deploy New Organisation Script
* Deploy installation files to correct folder.
  
  There should be default folders for frontend and backend keycloak installation files, where new realms can be deployed dynamically.

* Implement invite user on automatic deploy new organization script
    
    Deploy new organisation script does not allow (yet) for inviting the Admin user which is a nice to have 
    (just send an email and let the user setup his account and password)

### Resource and Scope Management

Not implemented yet, but new resources and scopes should be managed so the authorization manegement can be easier and 
cleaner on frontend side.

### Reduce Entitlements Json File

Right now, the fined grained authorization function only accepts the wild card `*` for full resources.
We should allow it for sub resources as well such as `satellite:*` and `system:*`

### Reduce AuthZ Token

On the auth-service, via request url we can be quite clear which service the request is accessing. The auth-service
should filter the entitlements by service to send to the resource server just the entiletments required by that service.

### Extend Fined Grained Authorization Function

So it can request multiple scopes on multiple resources.
This is more of an enhancement that can be made depending on if it is needed or not.
