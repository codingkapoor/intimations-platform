import lowercaseKeys from 'lowercase-keys';

import * as pushNotificationHandler from './pushNotificationsHandler';
import * as mailNotifications from './mailNotificationHandler';
import pool from '../database';
import { db } from '../config';

let notification = {};
const table = db.table;
let loggedInUser = {
    name: '',
    token: ''
}
let pushNotificationTokens = [];
let nameIdMap = {};

const formatDate = date => {

    let day = `${date.getDate()}`.padStart(2, 0);
    let month = `${date.getMonth() + 1}`.padStart(2, 0);
    let year = date.getFullYear();

    return year + '-' + month + '-' + day;
}

export const getNotificationMessage = message => {
    notification = {};
    let today = new Date();
    let tomorrow = new Date(today);
    let hasMoreThanOneRequest = message.requests.length === 1 ? false : message.requests.length > 1 ? true : '';
    tomorrow.setDate(tomorrow.getDate() + 1)
    today = formatDate(today);
    tomorrow = formatDate(tomorrow);
    getLoggedInUserDetails().then((res) => {
        setPushnotificationsTokensObj(res, message);
        let notificationDataObj = setNotificationDataObj(message);
        if (message.requests) {
            let item = message.requests[0];
            let dayText = (item.date === today) ? ' today ' : (item.date === tomorrow) ? ' tomorrow ' : '';
            notification.title = getNotificationTitle(loggedInUser.name, item, dayText, hasMoreThanOneRequest);
            notification.content = message.reason;
            pushNotificationHandler.handlePushTokens(notification,notificationDataObj,pushNotificationTokens);
            mailNotifications.sendMailNotification(notification);
            return;
        }
    })
}

const setPushnotificationsTokensObj = (response, message) => {
    loggedInUser = {
        name: '',
        token: ''
    }
    nameIdMap = {};
    pushNotificationTokens = [];
    let data = JSON.parse(JSON.stringify(response));
    data.forEach(element => {
        element = lowercaseKeys(element);

        // Save name id map for sending push notifications
        nameIdMap[element.id] = element.name;
        
        if (message.id === element.id) {
            loggedInUser.name = element.name;
            loggedInUser.token = element.token;
        }
        if (element.token !== loggedInUser.token) {
            pushNotificationTokens.push(element.token);
        }
    });
}

const getNotificationTitle = (name, item, day, hasMoreThanOneRequest) => {
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

export const getNotificationMessageForCancelledIntimation = message => {
    getLoggedInUserDetails().then((res) => {
        setPushnotificationsTokensObj(res, message);
        notification = {};
        let notificationDataObj = setNotificationDataObj(message);
        
        notification.title = `${loggedInUser.name} has cancelled Intimation`;
        notification.content = message.reason;
        pushNotificationHandler.handlePushTokens(notification, notificationDataObj, pushNotificationTokens);
        mailNotifications.sendMailNotification(notification);
    })
}

const setNotificationDataObj = (message) => {
    let notificationDataObj = {
        empId: message.id,
        empName: nameIdMap[message.id],
        reason: message.reason,
        lastModified: message.lastModified,
        requests: message.requests
    };
    return notificationDataObj;
}

