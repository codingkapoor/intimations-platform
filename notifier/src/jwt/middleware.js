import { jwtService } from '../jwt/service';

class JwtMiddleware {

    constructor() { }

    verify(req, res, next) {
        const bearer = req.header('Authorization') || '';
        const token = bearer.split(' ')[1];
        const valid = jwtService.verify(token);
        return valid ? next() : res.status(401).send({ error: 'Unauthorized' });
    }

    hasRole(role) {
        return (req, res, next) => {
            const bearer = req.header('Authorization') || '';
            const token = bearer.split(' ')[1];
            const decoded = jwtService.decode(token);
            const foundRole = decoded.payload.roles.find(e => e === role);
            return foundRole ? next() : res.status(403).send({ error: 'Access Denied' });
        }
    }

}

export let jwtMiddleware = new JwtMiddleware();