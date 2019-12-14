import express from 'express';
import kafka from 'kafka-node';
import * as kafkaStreams from './src/handlers/kafkaEventStreamHandler';

import indexRouter from './src/routes/index';
import tokenRouter from './src/routes/tokens';
import { kafkaConfig, expressServer } from './src/config/index'

const Consumer = kafka.Consumer;
const client = new kafka.KafkaClient(kafkaConfig.interface + ":" + kafkaConfig.port);
let app = express();

//Handle notifications

const consumer = new Consumer(client, [{ topic: kafkaConfig.consumer.topic, partition: kafkaConfig.consumer.partition }], { autoCommit: true });
consumer.on('message', (message) => {
    kafkaStreams.handleKafkaEvents(message);
});

// port on which you want to run your server 
const PORT_NUMBER = expressServer.port;
app.use(express.json());

// Routes
app.use('/', indexRouter);
app.use('/token', tokenRouter);

app.listen(PORT_NUMBER, () => {
    console.log('Server Online on Port' + PORT_NUMBER);
});

export default app;