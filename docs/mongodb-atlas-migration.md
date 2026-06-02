# MongoDB Atlas Migration

Use this before moving the application to EKS. Atlas removes MongoDB operations from the Kubernetes cluster so EKS can focus on stateless application rollout.

## 1. Create Atlas

Create a MongoDB Atlas cluster in the same AWS region as the application when possible, for example `ap-northeast-2`.

Create a database user and allow network access from the current EC2 public IP while testing. For production, prefer private networking or the narrowest possible CIDR.

## 2. Set Backend Environment

On EC2, edit `backend/.env`:

```env
SPRING_DATA_MONGODB_URI=mongodb+srv://<username>:<password>@<cluster-host>/skinai?retryWrites=true&w=majority
SPRING_MONGODB_URI=mongodb+srv://<username>:<password>@<cluster-host>/skinai?retryWrites=true&w=majority
```

Do not commit real Atlas credentials.

## 3. Restart Backend

From the repository root on EC2:

```bash
git pull origin dev
docker compose up -d --build backend
docker compose logs -f backend
```

Then verify:

```bash
curl http://localhost:8080/actuator/health
```

## 4. Confirm Data Writes

Run an analysis or chat flow that persists to MongoDB, then confirm the documents appear in Atlas.

At this stage the local `mongo` container may still run because the compose stack includes it, but backend reads the Atlas URI from environment variables. After Atlas is verified, remove the local MongoDB service and dependency from compose and Kubernetes manifests.
