// Runs ONCE, only when the Mongo volume is brand new (the official image's /docker-entrypoint-initdb.d).
// Creates the app user so a fresh machine (CI, a new server) works without manual steps.
// Least privilege: read/write on our 2 databases only, not admin.
db.getSiblingDB("admin").createUser({
  user: "softlaunch",
  pwd: process.env.MONGO_APP_PASSWORD,
  roles: [
    { role: "readWrite", db: "chat_db" },
    { role: "readWrite", db: "safety_db" }
  ]
});
print("created Mongo user 'softlaunch'");
