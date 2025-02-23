export default function LoginScreen() {
    return (
        <div>
          <button>sign in</button>
            <h1>login to MediAid</h1>
            <label>
                email:
                <input type="email" name="email" />
            </label>
            <label>
                password:
                <input type="password" name="password" />
            </label>
            <button>i forgot the password</button>
            <button>login</button>
            <button>connect by Google</button>
        </div>
    );
}