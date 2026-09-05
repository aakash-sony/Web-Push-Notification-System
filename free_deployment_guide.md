# 🚀 Free Deployment Guide: Deploying Spring Boot (Java 21) on Render

> **Senior Developer Note:** Welcome! This guide explains why Render shows only Docker instead of Java, how cloud containerization works behind the scenes, and provides a step-by-step walkthrough to deploy your **WebPush Backend** completely free without needing Docker installed on your computer.

---

## 1. Why is there no "Java" in Render's Language Dropdown?

When creating a Web Service on Render, you see runtimes like *Node, Python, Go, Ruby, Rust, and Elixir*, but **no Java**.

### The Reason:
- Render's native buildpacks only support a small set of interpreted or single-binary languages.
- For Java, .NET, PHP, and other enterprise languages, **Render delegates containerization to Docker**.

### 💡 The Big Misconception:
> *"My project doesn't use Docker, and I don't have Docker installed on my computer. Can I still deploy?"*
>
> **YES, ABSOLUTELY!** 
> 
> You **do NOT need Docker installed or running on your local machine**. 
> All you need is a simple text file named `Dockerfile` placed in your project's root folder. When you push your code to GitHub and connect it to Render, **Render’s cloud servers will read that file, build your Java 21 app, and run it inside their cloud container completely automatically.**

---

## 2. What We Have Already Configured in Your Project

To make your deployment painless, the following production-grade files have been set up in your project:

1. **`Dockerfile` (Root)**:
   - Uses a lightweight **Multi-Stage Build** (`eclipse-temurin:21-jdk-alpine` to compile, `eclipse-temurin:21-jre-alpine` to run).
   - Keeps image size tiny (~160MB instead of ~800MB).
   - Runs as a **non-root user** for security.
   - Dynamically binds to Render's dynamic `$PORT`.
   - Tuned with `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` so it **never crashes** Render's 512MB free tier RAM limit.
2. **`.dockerignore` (Root)**:
   - Excludes `target/`, `.git`, and IDE caches so Docker uploads only your source code in seconds.
3. **`application.properties` & `application-prod.properties`**:
   - Added `server.port=${PORT:8080}` to automatically bind to Render's dynamic port assignment.

---

## 3. Step-by-Step Free Deployment on Render

### Step 1: Push Your Code to GitHub
Ensure you commit and push the new files to your GitHub repository:
```bash
git add Dockerfile .dockerignore src/main/resources/application*.properties free_deployment_guide.md
git commit -m "chore: add Render docker config, port binding, and deployment guide"
git push origin main
```

---

### Step 2: Use Your Existing Neon Database (Recommended)

Since you already created your database on **[Neon](https://console.neon.tech/)**, **do NOT create a database on Render!**

> [!TIP]
> **Why Neon is better than Render DB:** Render's free PostgreSQL is automatically deleted after 30 days. Neon's free tier **does not expire**, provides modern serverless autoscaling, and is much more reliable for long-term projects. Connecting Render (for backend compute) to Neon (for database) is a standard production pattern.

#### How to get your credentials from Neon:
1. Open your Neon project: [Neon Console](https://console.neon.tech/app/projects/little-fog-95525512?database=neondb).
2. Look at the **Connection Details** box on your dashboard:
   - In the dropdown (where it says *psql* or *Node.js*), select **JDBC** or **Parameters only**.
   - You will see your credentials:
     - **Host**: e.g. `ep-little-fog-95525512.us-east-2.aws.neon.tech`
     - **Database**: `neondb`
     - **User**: (e.g. `neondb_owner`)
     - **Password**: (click the eye icon to reveal)
3. Your **Spring Boot JDBC URL** will look like this:
   ```text
   jdbc:postgresql://<YOUR_NEON_HOST>/neondb?sslmode=require
   ```
   *(Note: Neon requires SSL, so `?sslmode=require` at the end is mandatory).*

---

### Step 3: Create the Web Service on Render

1. On the Render Dashboard, click **New +** $\rightarrow$ **Web Service**.
2. Select **Build and deploy from a Git repository** $\rightarrow$ Click **Next**.
3. Connect your GitHub account and select your `Web-Push-Notification-System` repository.
4. Fill in the configuration:
   - **Name**: `webpush-backend`
   - **Region**: Pick the **same region** you selected for your PostgreSQL database.
   - **Branch**: `main`
   - **Root Directory**: Leave blank (root).
   - **Runtime**: Select **Docker** (Render will automatically detect the `Dockerfile` in the root).
   - **Instance Type**: Select **Free** (0.1 CPU, 512 MB RAM).

---

### Step 4: Configure Environment Variables

Scroll down to the **Environment Variables** section and add the following keys:

| Key | Recommended Value | Explanation |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates `application-prod.properties` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<HOST>:5432/<DATABASE>` | Your Render Postgres JDBC URL with `jdbc:` prefix |
| `SPRING_DATASOURCE_USERNAME` | `postgres` (or your Render DB user) | Postgres username |
| `SPRING_DATASOURCE_PASSWORD` | `<your-db-password>` | Postgres password |
| `ADMIN_USERNAME` | `admin` | Your chosen admin username |
| `ADMIN_PASSWORD` | `YourStrongAdminPassword123` | Password for admin authentication |
| `CORS_ALLOWED_ORIGINS` | `https://yourfrontend.com,http://localhost:5173` | Allowed frontend URL(s) for CORS |

---

### Step 5: Configure Firebase Credentials

Your application requires Firebase Admin SDK credentials. You have two secure options on Render:

#### Option A: Using Render's "Secret Files" (Recommended)
1. In your Web Service settings, go to the **Environment** tab $\rightarrow$ **Secret Files**.
2. Click **Add Secret File**:
   - **Filename**: `firebase-service-account.json`
   - **Contents**: Paste the entire JSON content of your Firebase service account key.
3. Render saves this file to `/etc/secrets/firebase-service-account.json`.
4. In your Environment Variables, set:
   - `FIREBASE_CREDENTIALS_PATH`: `/etc/secrets/firebase-service-account.json`

#### Option B: Classpath Fallback
- Your project already includes a fallback file in `src/main/resources/push-notification-5366b-firebase-adminsdk-fbsvc-cc7267adcb.json`. If `FIREBASE_CREDENTIALS_PATH` is left empty, the app will automatically pick this up from the classpath.

---

### Step 6: Deploy!

Click **Create Web Service**. 
- Render will pull your repository.
- It will execute the multi-stage `Dockerfile`.
- Maven will compile and package your `.jar`.
- The container will spin up and start your Spring Boot application.
- You will see the live logs in the Render console, ending with:
  ```text
  Started WebpushApplication in X.XXX seconds (process running for X.XXX)
  ```
- Your backend will now be live at `https://<your-service-name>.onrender.com`!

---

## 4. Senior Developer Insights & Free Tier Optimization

### 1. Dynamic Port Binding (`PORT`)
Render dynamically assigns a port (such as `10000`) and passes it in the `PORT` environment variable. 
If your app listens on hardcoded `8080`, Render's port-checker will fail and mark the build as timed out.
- **How we fixed it:** We added `server.port=${PORT:8080}` to your properties and Dockerfile so Spring Boot automatically listens on Render's assigned port.

### 2. Preventing 512MB Out-Of-Memory (OOM) Kills
Render Free Tier grants **512 MB of RAM**. Java 21's default heap allocation can attempt to grab more than 512MB, causing Linux to terminate the process (`Exit code 137`).
- **How we fixed it:** In the Dockerfile, we configured:
  ```dockerfile
  -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError
  ```
  This restricts the JVM heap to 75% (~384MB), leaving 128MB headroom for JVM Metaspace and OS threads.

### 3. Free Tier Inactivity Spin-Down (Cold Starts)
Render's free web services automatically spin down (sleep) after 15 minutes of zero traffic.
- When a new HTTP request hits your URL, it takes **30–50 seconds** to wake up and start Spring Boot.
- **Workaround to keep it awake:** You can set up a free monitor on [UptimeRobot](https://uptimerobot.com/) or [cron-job.org](https://cron-job.org/) that sends an HTTP `GET` request to your backend health endpoint every 10–14 minutes.

---

## 5. What If You Strictly Do Not Want a Dockerfile? (Free Alternatives)

If you specifically prefer a platform that detects `pom.xml` natively without any `Dockerfile`:

| Platform | Native Java (No Dockerfile)? | Free Tier Details | Database Included? |
| :--- | :--- | :--- | :--- |
| **Render** | ❌ Requires Dockerfile | Free Web Service (512MB RAM) + Free Postgres | Yes (Free Postgres 1GB) |
| **Railway.app** | ✅ Yes (Nixpacks auto-builds `pom.xml`) | $5 trial credit / monthly usage limit | Yes (PostgreSQL plugin) |
| **Koyeb** | ✅ Yes (Cloud Native Buildpacks) | 1 free nano service (512MB RAM) | Free Postgres via Neon integration |
| **Fly.io** | ❌ Prefers Dockerfile | Free allowance for lightweight apps | Fly Postgres |

> **Verdict:** Render with a `Dockerfile` remains the most stable, reproducible, and widely adopted free setup for Spring Boot. Having a `Dockerfile` in your repository is standard industry practice for modern backend developers.
