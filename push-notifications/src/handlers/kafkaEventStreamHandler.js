let pool = require('../../database');
let pushNotificationTemplate = require('./pushNotificationTemplateHandler');
let db_config = require('../../db_config');
const table = db_config.table;

handleKafkaEvents = function(message){
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

module.exports.handleKafkaEvents = handleKafkaEvents;