import express from 'express';
import kafka from 'kafka-node';
import * as kafkaStreams from './src/handlers/kafkaEventStreamHandler';
import indexRouter from './src/routes/index';
import tokenRouter from './src/routes/tokens';

const Consumer = kafka.Consumer;
const client = new kafka.KafkaClient("localhost:2181");
let app = express();

//Handle push notifications

const consumer = new Consumer(client, [ { topic: 'employee', partition: 0 } ], { autoCommit: true });
consumer.on('message', (message) => {
    kafkaStreams.handleKafkaEvents(message);
});

const PORT_NUMBER = 3000; // port on which you want to run your server on

app.use(express.json());

// Routes
app.use('/', indexRouter);
app.use('/token', tokenRouter);

app.listen(PORT_NUMBER, () => {
    console.log('Server Online on Port' + PORT_NUMBER);
});

export default app;