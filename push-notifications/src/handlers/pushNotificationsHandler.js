const { Expo } = require('expo-server-sdk');
let pool = require('../../database');
let pushNotificationTemplate = require('./pushNotificationTemplateHandler');

const expo = new Expo();

// let savedPushTokens = ['ExponentPushToken[P6Hm9oEChVDz7OPwO-_Qsj]'];

let savedPushTokens = [];

const handlePushTokens = (message) => {
    pool.query('SELECT * FROM appTokens', function(err, response){
        savedPushTokens = JSON.parse(JSON.stringify(response));
        savedPushTokens = savedPushTokens.map(element => {
            return element.token;
        });
        let notifications = [];
        for (let pushToken of savedPushTokens) {
          if (!Expo.isExpoPushToken(pushToken)) {
            console.error(`Push token ${pushToken} is not a valid Expo push token`);
            continue;
          }
          notifications.push({
            to: pushToken,
            sound: 'default',
            title: message.title,
            body: message.content,
            data: message
          })
        }
        let chunks = expo.chunkPushNotifications(notifications);
        (async () => {
            for (let chunk of chunks) {
                try {
                    let receipts = await expo.sendPushNotificationsAsync(chunk);
                    console.log(receipts);
                } catch (error) {
                    console.error(error);
                }
            }
        })(); 
    });
}

module.exports.handlePushTokens = handlePushTokens;