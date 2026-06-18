# Ngrok Guide — Exposing ByteHR AI for Teams Local Testing

Microsoft Teams requires a **publicly accessible HTTPS endpoint** for bot messaging.
During local development, use [ngrok](https://ngrok.com) to create a secure tunnel.

---

## 1. Install Ngrok

```bash
# macOS
brew install ngrok

# Linux
curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null
echo "deb https://ngrok-agent.s3.amazonaws.com buster main" | sudo tee /etc/apt/sources.list.d/ngrok.list
sudo apt update && sudo apt install ngrok

# Or download from https://ngrok.com/download
```

---

## 2. Authenticate

```bash
ngrok config add-authtoken <your-ngrok-authtoken>
```

Get your token from [https://dashboard.ngrok.com](https://dashboard.ngrok.com).

---

## 3. Start the Tunnel

```bash
ngrok http 8080
```

You will see output like:
```
Forwarding  https://a1b2c3d4.ngrok-free.app -> http://localhost:8080
```

Copy the **https** URL.

---

## 4. Update Azure Bot Endpoint

1. Go to [Azure Portal](https://portal.azure.com) → Your Bot → **Configuration**
2. Set **Messaging endpoint** to:
   ```
   https://a1b2c3d4.ngrok-free.app/api/messages
   ```
3. Save

> **Note:** The ngrok URL changes every time you restart ngrok on a free plan.
> Update the Azure Bot endpoint whenever you restart the tunnel.
> For persistent URLs, use a paid ngrok plan or deploy to a cloud service.

---

## 5. Test

Send a message to your ByteHR AI bot in Teams.
You should see the request hit your local Spring Boot server.
