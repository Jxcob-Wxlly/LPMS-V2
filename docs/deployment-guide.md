# Deployment guide - Azure SQL, Azure App Service and the Android app

Do these steps once, in order. Everything can be done in the Azure portal (<https://portal.azure.com>) with a student subscription
(Azure for Students) - no command line needed.

> **Cost tip:** use the **Free (F1)** App Service plan and an Azure SQL **serverless / free offer** database, and delete the
> resource group when the module is over. A free App Service sleeps when idle, so the first request after a pause can take about
> a minute - open `/api/health` in a browser a minute before the demo.

## 1. Create the Azure SQL database

1. Portal > **Create a resource** > **SQL Database**.
2. **Resource group:** create `rg-lpms`. **Database name:** `LpmsDb`.
3. **Server:** *Create new* > name e.g. `lpms-sql-<yourname>`, location *South Africa North* (or nearest), authentication
   **SQL authentication**, set an admin login and a strong password (**write it down**).
4. **Compute + storage:** choose the cheapest option (serverless or the free offer).
5. **Networking** tab: *Allow Azure services and resources to access this server* = **Yes**, and
   *Add current client IP address* = **Yes**.
6. **Review + create**.

### Create the tables and load the data

1. Open the database > **Query editor (preview)** and sign in with the SQL admin login.
2. Paste the contents of `database/01-create-schema.sql` and **Run**.
3. Paste `database/02-seed-data.sql` and **Run** (finishes with `LPMS seed data loaded.`).
4. Paste `database/03-demo-queries.sql` query 1 and **Run** - every table must show at least 10 rows.

If the Query editor cannot connect, add your IP under **SQL server > Networking > Firewall rules**.

### Connection string

SQL database > **Connection strings** > **ADO.NET (SQL authentication)**. Copy it and replace `{your_password}`:

```
Server=tcp:lpms-sql-xxxx.database.windows.net,1433;Initial Catalog=LpmsDb;User ID=lpmsadmin;Password=YOUR-PASSWORD;Encrypt=True;TrustServerCertificate=False;Connection Timeout=30;
```

## 2. Create the App Service for the API

1. Portal > **Create a resource** > **Web App**.
2. **Resource group:** `rg-lpms`. **Name:** e.g. `lpms-api-<yourname>` (this becomes `https://lpms-api-<yourname>.azurewebsites.net`).
3. **Publish:** Code. **Runtime stack:** **.NET 8 (LTS)**. **Operating system:** Linux. **Pricing plan:** Free F1 (or Basic B1 for no sleeping).
4. **Review + create**.

### Application settings (secrets live here, never in Git)

Web App > **Settings > Environment variables** (or *Configuration*):

| Type | Name | Value |
|---|---|---|
| **App setting** | `Jwt__Key` | a long random string, at least 32 characters (e.g. generate with a password manager) |
| **App setting** | `ASPNETCORE_ENVIRONMENT` | `Production` |
| **Connection string** (type **SQLAzure**) | `LpmsDb` | the connection string from step 1 |
| App setting (optional) | `Cors__AllowedOrigins__0` | `https://<your-github-username>.github.io` (the website's address) |

Click **Apply** and let the app restart.

## 3. Deploy the API

### Option A - GitHub Actions (recommended, shows the "Deploy" stage)

1. Web App > **Overview** > **Get publish profile** (downloads an XML file).
2. GitHub repository > **Settings > Secrets and variables > Actions**:
   - **Secrets > New repository secret:** `AZURE_WEBAPP_PUBLISH_PROFILE` = the *entire contents* of the XML file.
   - **Variables > New repository variable:** `AZURE_WEBAPP_NAME` = your web app name (e.g. `lpms-api-yourname`).
3. Push to `main`. The **CI** workflow builds, tests and deploys, then calls `/api/health`.

If the Web App has *Basic authentication* disabled, enable it: Web App > **Configuration > General settings > SCM Basic Auth Publishing Credentials = On**.

### Option B - Azure DevOps pipeline

1. Azure DevOps > **Project settings > Service connections > New > Azure Resource Manager** (automatic). Note its name.
2. **Pipelines > New pipeline > GitHub** (or Azure Repos) > *Existing Azure Pipelines YAML file* > `/azure-pipelines.yml`.
3. **Variables:** `azureSubscription` = the service connection name, `webAppName` = your web app name. Run the pipeline.

### Option C - one-off publish from your PC

```bash
cd api/src/Lpms.Api
dotnet publish -c Release -o publish
cd publish && zip -r ../api.zip . && cd ..
# Web App > Advanced Tools (Kudu) > Tools > Zip Push Deploy: drag api.zip in, or:
az webapp deploy --resource-group rg-lpms --name lpms-api-yourname --src-path api.zip --type zip
```

## 4. Check the API is live

Open these in a browser (replace the name):

- `https://lpms-api-yourname.azurewebsites.net/api/health` - should show `"status":"Healthy","database":"Connected"`.
- `https://lpms-api-yourname.azurewebsites.net/swagger` - the interactive API documentation. Use **POST /api/auth/login** with
  `{"identifier":"teacher01","password":"Lpms@2026!"}`, copy the `token`, click **Authorize**, paste the token (without the word *Bearer*) and confirm, then try **GET /api/sessions/mine**.

If `/api/health` says **Unhealthy** or the site shows HTTP 500: check the connection string name is exactly `LpmsDb`, its type is **SQLAzure**,
the SQL firewall allows Azure services, and `Jwt__Key` is set. Logs: Web App > **Monitoring > Log stream**.

## 5. Build and install the Android app

1. Push to GitHub. In **Actions**, open the latest **CI** run and download the artifact **lpms-teacher-debug-apk** (a zip containing
   `app-debug.apk`; unzip it).
2. Send `app-debug.apk` to the phone (USB, Google Drive, WhatsApp "document", email) and open it. Allow *Install unknown apps* if asked.
3. Open the app. If the default address is not yours, tap **Server settings** on the sign-in screen, enter
   `https://lpms-api-yourname.azurewebsites.net/`, tap **Save and test** (should say *Connected*).
4. Sign in with `teacher01` / `Lpms@2026!`.

To bake your address into the APK instead, set `lpmsApiBaseUrl` in `android/gradle.properties` before building.

### Running from Android Studio on a phone

Enable **Developer options > USB debugging**, plug in the phone, choose it in the device list and press **Run**.
For an API running on your PC use `http://<PC-IP>:5080/` (same Wi-Fi), allow port 5080 in Windows Firewall
(`netsh advfirewall firewall add rule name="LPMS API" dir=in action=allow protocol=TCP localport=5080`) and run the API with
`dotnet run` (it listens on all interfaces).

## 6. Before recording the demo video

- [ ] `/api/health` is Healthy (wakes the free App Service).
- [ ] Azure portal tabs open: Web App overview, SQL database Query editor (signed in), GitHub Actions run, Azure DevOps board.
- [ ] Re-run `02-seed-data.sql` the morning of the demo so there are sessions today and some still needing attendance.
- [ ] Phone charged, screen recording on, app signed out.

## 7. Cleaning up

Delete the resource group `rg-lpms` (this removes the web app and database) once your marks are released.
