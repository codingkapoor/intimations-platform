import fs from 'fs';
import jwt from 'jsonwebtoken';

const publicKey = fs.readFileSync('public.key', 'utf8');

class JwtService {

    constructor() { }

    verify(token) {
        try {
            return jwt.verify(token, publicKey);
        } catch (error) {
            console.log(error);
            return false;
        }
    }

    decode(token) {
        return jwt.decode(token, { complete: true });
    }

}

export let jwtService =  new JwtService();