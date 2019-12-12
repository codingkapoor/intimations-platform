import lowercaseKeys from 'lowercase-keys';

import * as pushNotificationHandler from './pushNotificationsHandler';
import pool from '../database';
import { db } from '../config';

let pushNotification = {};
const table = db.table;
let savedPushTokens = [];
let loggedInUser = {
    name: '',
    token: ''
}
let pushNotificationTokens = [];

const formatDate = date => {

    let day = `${date.getDate()}`.padStart(2, 0);
    let month = `${date.getMonth() + 1}`.padStart(2, 0);
    let year = date.getFullYear();

    return year + '-' + month + '-' + day;
}

export const getPushNotificationMessage = (message) => {
    pushNotification = {};
    let today = new Date();
    let tomorrow = new Date(today);
    let hasMoreThanOneRequest = message.requests.length === 1 ? false : message.requests.length > 1 ? true : '';
    tomorrow.setDate(tomorrow.getDate() + 1)
    today = formatDate(today);
    tomorrow = formatDate(tomorrow);
    getLoggedInUserDetails().then((res)=>{
        setPushnotificationsTokensObj(res, message);
        if (message.requests) {
            let item = message.requests[0];
            let dayText = (item.date === today) ? ' today ' : (item.date === tomorrow) ? ' tomorrow ' : '';
            pushNotification.title = getPushNotificationTitle(loggedInUser.name, item, dayText, hasMoreThanOneRequest);
            pushNotification.content = message.reason;
            pushNotificationHandler.handlePushTokens(pushNotification, pushNotificationTokens);
            return;
        }
    })
}

const setPushnotificationsTokensObj = (response, message) => {
    loggedInUser = {
        name: '',
        token: ''
    }
    pushNotificationTokens = [];
    savedPushTokens = JSON.parse(JSON.stringify(response));
    savedPushTokens.forEach(element => {
        element = lowercaseKeys(element);
        if(message.id === element.id){
            loggedInUser.name = element.name;
            loggedInUser.token = element.token;
        }
        if(element.token !== loggedInUser.token){
            pushNotificationTokens.push(element.token);
        }
    });
}

const getPushNotificationTitle = (name, item, day, hasMoreThanOneRequest) => {
    let appendFirstValText = (item.firstHalf === 'Leave') ? ' on ' : '';
    let appendSecondValText = (item.secondHalf === 'Leave') ? ' on ' : '';
    let appendText = hasMoreThanOneRequest ? ' and has planned leaves/WFH for subsequent days ' : '';
    let messageText = '';
    if (day) {
        (item.firstHalf === item.secondHalf) ? messageText = name + ' is ' + appendFirstValText + item.firstHalf + day + appendText :
            (item.firstHalf === 'WFO') ? messageText = name + ' is ' + appendSecondValText + item.secondHalf + ' in second half ' + day + appendText :
                (item.secondHalf === 'WFO') ? messageText = name + ' is ' + appendFirstValText + item.firstHalf + ' in first half ' + day + appendText :
                    messageText = name + ' is ' + appendFirstValText + item.firstHalf + ' in first half and ' + appendSecondValText + item.secondHalf + ' in second half ' + day + appendText;
        return messageText.replace(/\s+/g, ' ').trim();
    } else {
        return name + ' has planned WFH/Leaves for subsequent days.';
    }
}

const getLoggedInUserDetails = () => {
    return new Promise((resolve, reject) => {
        pool.query(`SELECT * FROM ${table}`, (err, response) => {
            if (err) throw err;
            resolve(response);
        });
    })
}
 
export const getPushNotificationMessageForCancelledIntimation = (message) => {
    getLoggedInUserDetails().then((res)=>{
        setPushnotificationsTokensObj(res, message);
        pushNotification = {};
        pushNotification.title = `${loggedInUser.name} has cancelled Intimation`;
        pushNotification.content = message.reason;
        pushNotificationHandler.handlePushTokens(pushNotification, pushNotificationTokens);
    })
}

