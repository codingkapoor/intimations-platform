import express from 'express';
import pool from '../database';
import lowercaseKeys from 'lowercase-keys';

import { db } from '../config/index';

let router = express.Router();
let savedPushTokens = [];
const table = db.table;

const saveToken = (req) => {
  pool.query(`SELECT * FROM ${table}`, (err, response) => {
    savedPushTokens = JSON.parse(JSON.stringify(response));
    savedPushTokens.forEach(element => {
      element = lowercaseKeys(element);
      if ((element.id === parseInt(req.params.id) && !element.token.length) ||
        (element.id === parseInt(req.params.id) && element.token != req.body.token)) {
        pool.query(`UPDATE ${table} SET token = "${req.body.token}" WHERE ID = ${req.params.id}`, (err, res) => {
          if (err) throw err;
          console.log("1 record updated");
        });
      }
    });
  });
}

const removeToken = (req) => {
  pool.query(`UPDATE ${table} SET token = "" WHERE ID = ${req.params.id}`, (err, res) => {
    if (err) throw err;
    console.log("1 record updated");
  });
}

router.post('/register/:id', (req, res) => {
  saveToken(req);
  console.log(`Received push token, ${req.body.token}`);
  res.send(`Received push token, ${req.body.token}`);
});

router.post('/deregister/:id', (req, res) => {
  removeToken(req);
  console.log(`Received employee to deregister , ${req.params.id}`);
  res.send(`Received employee to deregister, ${req.params.id}`);
});

export default router;