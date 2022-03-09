import React, { useState, useContext, useEffect } from 'react';
import { useHistory } from 'react-router-dom';
import { AccountContext } from './Auth/Accounts';

import {Container, Row, Col, Button} from 'react-bootstrap';

const Banner = () =>{

  const history = useHistory();

  const { LoggedIn, logout } = useContext(AccountContext);

  const handleLogout = () => {
    logout()
    history.push("/Login")
  }
  return(
    <header>
      <Container>
        <Row>
        <Col sm="11"><h1>Polly Notes Application</h1></Col>
        <Col sm="1">
          {LoggedIn ? (
            <Container>
              <Button size="sm" onClick={handleLogout}>Logout</Button>
            </Container>
          ) : ''}
        </Col>
        </Row>
      </Container>
    </header>
  )
};

export default Banner;