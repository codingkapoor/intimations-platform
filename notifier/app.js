import express from 'express';
import kafka from 'kafka-node';
import * as kafkaStreams from './src/handlers/kafkaEventStreamHandler';

import indexRouter from './src/routes/index';
import tokenRouter from './src/routes/tokens';
import { kafkaConfig } from './src/config'

const Consumer = kafka.Consumer;
const client = new kafka.KafkaClient(kafkaConfig.interface + ":" + kafkaConfig.port);
let app = express();

//Handle notifications

const consumer = new Consumer(client, [{ topic: kafkaConfig.consumer.topic, partition: kafkaConfig.consumer.partition }], { autoCommit: true });
consumer.on('message', (message) => {
    kafkaStreams.handleKafkaEvents(message);
});

// port on which you want to run your server 
app.use(express.json());

// Routes
app.use('/', indexRouter);
app.use('/token', tokenRouter);

// catch 404 and forward to error handler
app.use(function (req, res, next) {
    var err = new Error('Not Found');
    err.status = 404;
});

// error handler
app.use(function (err, req, res, next) {
    res.status(err.status || 500);
    res.render('error', {
        message: err.message,
        error: (app.get('env') === 'development') ? err : {}
    });
});

export default app;