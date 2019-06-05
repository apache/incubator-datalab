use dlabdb
db.createUser(
    {
      user: "admin",
      pwd: "PASSWORD",
      roles: [{'role':'userAdminAnyDatabase','db':'admin'}]
    }
);
