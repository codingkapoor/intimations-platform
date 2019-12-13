import express from 'express';
let router = express.Router();

router.get('/', (req, res) => {
    res.send('Push Notification Server Running');
});


export default router;