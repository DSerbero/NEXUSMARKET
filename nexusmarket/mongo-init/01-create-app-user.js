const databaseName = process.env.MONGO_INITDB_DATABASE;
const applicationDatabase = db.getSiblingDB(databaseName);

applicationDatabase.createUser({
  user: process.env.MONGODB_APP_USERNAME,
  pwd: process.env.MONGODB_APP_PASSWORD,
  roles: [{ role: "readWrite", db: databaseName }]
});