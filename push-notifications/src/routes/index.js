let express = require('express');
let router = express.Router();

router.get('/', (req, res) => {
    res.send('Push Notification Server Running'); 
});

module.exports = router;