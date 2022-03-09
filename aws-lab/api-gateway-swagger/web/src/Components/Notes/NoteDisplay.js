import {useEffect, useState} from 'react';
import {Container, Row, Col, Table, InputGroup, FormControl, Button, Form} from 'react-bootstrap';
import { HiSpeakerphone, HiOutlineTrash } from 'react-icons/hi';
import { GrEdit } from 'react-icons/gr';
import LoadingSpinner from '../LoadingSpinner';

let NoteDisplay = (props) =>{

  const [query, setQuery] = useState("")
  const [apiQuery, setApiQuery] = useState("")
  const [firstRun, setFirstRun] = useState(true)
  const [selAccent, setSelAccent] = useState("american")
  const [voice, setVoice] = useState("")
  const queryDelay = .25;

  const accents = {
    american: {display: 'American', voice: [{name: 'Joey'}, {name: 'Joanna'}]},
    australian: {display: 'Australian', voice: [{name: 'Russell'}, {name: 'Nicole'}]},
    british: {display: 'British', voice: [{name: 'Brian'}, {name: 'Amy'}]},
    indian: {display: 'Indian', voice: [{name: 'Raveena'}]},
    welsh:  {display: 'Welsh', voice: [{name: 'Geraint'}]}
}

  const handleDelete = (id) => {
    props.noteFunctions.deleteNote(id)
  }

  const handleSpeak = (id) => {
    props.noteFunctions.speakNote(id, voice)
  }
  
  const handleEdit = (id) => {
    props.noteFunctions.handleSelectNote(id)
  }
  
  useEffect(async () => {
    if (!firstRun) {
    props.noteFunctions.filterNotes(apiQuery)
    }
  }
  , [apiQuery])

  useEffect(() => {
    let timer1 = setTimeout(() => {
      if (!firstRun) {
        setApiQuery(query);
      } else {setFirstRun(false)}
    }, queryDelay * 1000);
    return () => {
      clearTimeout(timer1);
    }
  }, [query])

  useEffect((() => {
    setVoice(accents[selAccent].voice[0].name)
  }), [selAccent])

  useEffect(() => {
    console.log(voice)
  }, [voice])

  const NoteRows = props.notes.map(note => {
    return(
      <tr key={note.NoteId}>
        <td><Button active={(props.selNote.NoteId === note.NoteId) ? true : false}  variant="outline-info" onClick={() => handleEdit(note.NoteId)}><GrEdit /></Button></td>
        <td>{note.NoteId}</td>
        <td width="70%">{note.Note}</td>
        <td width="20%" align="right"><Button onClick={() => handleSpeak(note.NoteId)} variant="outline-info" ><HiSpeakerphone/></Button><Button onClick={() => handleDelete(note.NoteId)} variant="outline-info" >{(props.deleting == note.NoteId) ? <LoadingSpinner /> : <HiOutlineTrash/>}</Button></td>
      </tr>
    )
  })
  const languageOptions = Object.entries(accents).map(item => <option key={item[0]} value={item[0]}>{item[1]['display']}</option>)
  let voiceOptions = accents[selAccent].voice.map((item, i) => <option key={i} value={item.name}>{item.name}</option>)
  
  return(
    <Container>
      {}
      <InputGroup className="mb-3">
        <InputGroup.Prepend>
          <InputGroup.Text id="search-heading">Search Notes:</InputGroup.Text>
        </InputGroup.Prepend>
        <FormControl
          placeholder="Search for"
          aria-label="SearchBox"
          aria-describedby="search-heading"
          value={query} onChange={e => setQuery(e.target.value)}
        />
        <InputGroup.Append>
          <InputGroup.Text id="search-heading">{props.searching ? <LoadingSpinner /> : null}</InputGroup.Text>
        </InputGroup.Append>
      </InputGroup>
      <Table striped borderless hover size="sm">
        <thead>
          <tr>
            <th></th>
            <th>ID</th>
            <th>Note</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {NoteRows}
        </tbody>
        <tfoot>
          <tr>
            <td colSpan="4">
              <Row>
                <Col lg="7"></Col>
                <Col lg="5">
                <Form inline>
                  <Form.Group>
                    <Form.Label srOnly>
                      Voice Accent
                    </Form.Label>
                    <InputGroup size="sm">
                      <InputGroup.Prepend size="sm">
                        <InputGroup.Text>Polly Accent and Voice:</InputGroup.Text>
                      </InputGroup.Prepend>
                      <Form.Control as="select" size="sm" value={selAccent} onChange={e => setSelAccent(e.target.value)}>
                        {languageOptions}
                      </Form.Control>
                      <Form.Control as="select" size="sm" value={voice} onChange={e => setVoice(e.target.value)}>
                        {voiceOptions}
                      </Form.Control>
                    </InputGroup>
                  </Form.Group>
                  </Form>
                </Col>
              </Row>
              
            </td>
          </tr>
        </tfoot>
      </Table>
    </Container>
  )
};

export default NoteDisplay;