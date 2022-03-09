import React, { useState, useContext } from "react";
import { AccountContext } from '../Components/Auth/Accounts';
import {Container, Form, Button, InputGroup} from 'react-bootstrap';
import { Redirect, useHistory } from 'react-router-dom';
import LoadingSpinner from '../Components/LoadingSpinner';

export default (props) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loggingIn, setLoggingIn] = useState(false);
  const history = useHistory();

  const { authenticate, LoggedIn } = useContext(AccountContext);

  const onSubmit = event => {
    event.preventDefault();
    setLoggingIn(true)
    authenticate(username, password)
      .then(data => {
        history.push("/")
      })
      .catch(err => {
        setLoggingIn(false)
      })
  };

  return (
    <Container>
      {(LoggedIn) ? <Redirect to='/' /> :
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
        <Button variant="primary" type="submit">{(loggingIn) ? <LoadingSpinner /> : 'Login'}</Button>
      </Form>
    }
    </Container>
    
  );
};