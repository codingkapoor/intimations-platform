let express = require('express');
let app = express();
let pool = require('./database');

let kafka = require('kafka-node');
let kafkaStreams = require('./src/handlers/kafkaEventStreamHandler');
const Consumer = kafka.Consumer;
const client = new kafka.KafkaClient("localhost:2181");

//Handle push notifications

const consumer = new Consumer(client, [ { topic: 'employee', partition: 0 } ], { autoCommit: true });
consumer.on('message', function (message) {
    kafkaStreams.handleKafkaEvents(message);
});

const indexRouter = require('./src/routes/index');
const tokenRouter = require('./src/routes/tokens');

const PORT_NUMBER = 3000; // port on which you want to run your server on

app.use(express.json());

// Routes
app.use('/', indexRouter);
app.use('/token', tokenRouter);

app.listen(PORT_NUMBER, () => {
    console.log('Server Online on Port' + PORT_NUMBER);
});

module.exports = app;