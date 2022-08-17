Development
===========
This page contains information for developers about how to contribute to this project.

Set-up
------
This project can be used in combination with  [dans-dev-tools](https://github.com/DANS-KNAW/dans-dev-tools#dans-dev-tools). Before you can start it as a service
some dependencies must first be started:

### HSQL database

Open a separate terminal tab:
```commandline
start-hsqldb.sh
```

### easy-validate-dans-bag (old)

**Note: this subsection can be removed when `dd-validate-dans-bag` is finished**

Open a separated terminal and cd to the `easy-validate-dans-bag-project`:

```commandline
run-reset-env.sh
run-service.sh
```

### dd-validate-dans-bag

Open a separated terminal and cd to the `dd-validate-dans-bag-project`:

```commandline
start-env.sh
start-service.sh
```

### dd-dtap
The service needs a Dataverse instance to talk to. For this you can use [dd-dtap](https://github.com/DANS-KNAW/dd-dtap): 

```commandline
start-preprovisioned-box -s
```

After start-up:
```commandline
vagrant ssh
curl -X PUT -d s3kretKey http://localhost:8080/api/admin/settings/:BlockedApiKey
curl -X PUT -d unblock-key http://localhost:8080/api/admin/settings/:BlockedApiPolicy
```

### dd-ingest-flow
```commandline
start-env.sh
```
Configure the correct API key

Now you can start the service:
```commandline
start-service.sh
```
