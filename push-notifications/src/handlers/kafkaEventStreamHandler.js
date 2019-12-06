import pool from '../conf/database';
import {db} from '../conf/config';
import * as pushNotificationTemplate from './pushNotificationTemplateHandler';
const table = db.table;

export const handleKafkaEvents = (message) => {
    let data = JSON.parse(message.value);
    switch(data.type){
        case 'EmployeeAddedKafkaEvent':
            pool.query(`INSERT INTO ${table} (id,token) VALUES ('${data.id}', '')` , (err, res) => {
                if (err) throw err; 
                console.log("1 record inserted");  
            });
            break;
        case 'EmployeeDeletedKafkaEvent':
        case 'EmployeeTerminatedKafkaEvent':
            pool.query(`DELETE FROM ${table} WHERE ID = ${data.id}` , (err, res) => {
                if (err) throw err; 
                console.log("1 record deleted");  
            });
            break;
        case 'IntimationCreatedKafkaEvent':
        case 'IntimationUpdatedKafkaEvent':
            pushNotificationTemplate.getPushNotificationMessage(data);
            break;
        case 'IntimationCancelledKafkaEvent':
            pushNotificationTemplate.getPushNotificationMessageForCancelledIntimation(data);
            break;
        default:
            // do nothing for now
    }
}
