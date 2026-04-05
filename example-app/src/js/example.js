import { Enroll } from 'enroll-capacitor-plugin';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    Enroll.echo({ value: inputValue })
}
