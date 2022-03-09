import React, { useState } from 'react';
import {Container, Form, Button, InputGroup} from 'react-bootstrap';
import UserPool from '../UserPool';

export default () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const onSubmit = event => {
    event.preventDefault();

    UserPool.signUp(username, password, [], null, (err, data) => {
      if (err) console.error(err);
      console.log(data);
    });
  };

  return (
    <Container>
      <Form onSubmit={onSubmit}>
        <Form.Group controlId="username">
          <Form.Label srOnly>
            Username:
          </Form.Label>
          <InputGroup>
            <InputGroup.Prepend>
                <InputGroup.Text>Username:</InputGroup.Text>
              </InputGroup.Prepend>
              <Form.Control value={username} onChange={event => setUsername(event.target.value)}></Form.Control>
          </InputGroup>
        </Form.Group>
        <Form.Group controlId="password">
          <Form.Label srOnly>
            Password:
          </Form.Label>
          <InputGroup>
            <InputGroup.Prepend>
                <InputGroup.Text>Password:</InputGroup.Text>
              </InputGroup.Prepend>
              <Form.Control type="password" value={password} onChange={event => setPassword(event.target.value)}></Form.Control>
          </InputGroup>
        </Form.Group>

        <Button variant="primary" type="submit">Signup</Button>
      </Form>
    </Container>
  );
};