import { Expo } from 'expo-server-sdk';
import pool from '../conf/database';
import { db } from '../conf/config';

// let savedPushTokens = ['ExponentPushToken[P6Hm9oEChVDz7OPwO-_Qsj]'];

const expo = new Expo();
const table = db.table;
let savedPushTokens = [];

export const handlePushTokens = (message) => {
    pool.query(`SELECT * FROM ${table}`, (err, response) => {
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
