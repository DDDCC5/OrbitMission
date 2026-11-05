import {Client} from '/Users/chanzungsan/.local/lib/nodejs/node-v22.23.2-darwin-arm64/lib/node_modules/@picoxr/pico-cli/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js';
import {StdioClientTransport} from '/Users/chanzungsan/.local/lib/nodejs/node-v22.23.2-darwin-arm64/lib/node_modules/@picoxr/pico-cli/node_modules/@modelcontextprotocol/sdk/dist/esm/client/stdio.js';
import fs from 'node:fs';
const client=new Client({name:'orbit-mission-asset-authoring',version:'1.0.0'});
const transport=new StdioClientTransport({command:'/Users/chanzungsan/.local/lib/nodejs/node-v22.23.2-darwin-arm64/bin/pico-cli',args:['editor:bootstrap'],stderr:'inherit'});
try {await client.connect(transport); const req=JSON.parse(fs.readFileSync(process.argv[2],'utf8')); const result=req.name?await client.callTool(req):await client.listTools();fs.writeFileSync(process.argv[3],JSON.stringify(result,null,2)); console.log(JSON.stringify(result));} finally {await client.close();}
