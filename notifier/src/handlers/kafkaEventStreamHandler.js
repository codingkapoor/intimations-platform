import pool from '../database';
import { db } from '../config';
import * as notificationTemplate from './notificationTemplateHandler';
const table = db.table;

export const handleKafkaEvents = (message) => {
    let data = JSON.parse(message.value);
    switch (data.type) {
        case 'EmployeeAddedKafkaEvent':
            pool.query(`INSERT INTO ${table} (empId,token,name) VALUES ('${data.id}', null, '${data.name}')`, (err, res) => {
                if (err) throw err;
                console.log("1 record inserted");
            });
            break;
        case 'EmployeeDeletedKafkaEvent':
        case 'EmployeeTerminatedKafkaEvent':
            pool.query(`DELETE FROM ${table} WHERE empId = ${data.id}`, (err, res) => {
                if (err) throw err;
                console.log("1 record deleted");
            });
            break;
        case 'IntimationCreatedKafkaEvent':
        case 'IntimationUpdatedKafkaEvent':
            notificationTemplate.getNotificationMessage(data);
            break;
        case 'IntimationCancelledKafkaEvent':
            notificationTemplate.getNotificationMessageForCancelledIntimation(data);
            break;
        default:
        // do nothing for now
    }
}
