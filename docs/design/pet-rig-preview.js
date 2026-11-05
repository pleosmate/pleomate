const images = {};
const views = [];
const labels = ['정면', '왼쪽', '오른쪽', '뒷면', '앞 왼쪽', '앞 오른쪽'];
let playing = !matchMedia('(prefers-reduced-motion: reduce)').matches;
let elapsed = 0, lastTime = 0, selectedPhase = null;
const mode = document.querySelector('#motion');
const direction = document.querySelector('#direction');
const toggle = document.querySelector('#toggle');
function updateControls() {
  toggle.textContent = playing ? '일시정지' : '재생';
  direction.disabled = mode.value !== 'run';
  document.querySelector('#phase-label').textContent = selectedPhase === null ? '자동 재생' : Math.round(selectedPhase * 100) + '%';
}
toggle.onclick = () => { playing = !playing; if (playing) selectedPhase = null; updateControls(); };
mode.onchange = () => { elapsed = 0; selectedPhase = null; updateControls(); };
document.querySelector('#phase').oninput = event => {
  selectedPhase = Number(event.target.value); playing = false; updateControls();
};
updateControls();
for (const [id, label] of [['bear', '곰'], ['rabbit', '토끼']]) {
  const image = new Image(); image.src = '../../app/src/main/assets/pets/rig/' + id + '.png'; images[id] = image;
  const limbs = new Image(); images[id + 'limbs'] = limbs;
  limbs.onload = () => {
    const r = rigDefinitions[id].limbs[0], paw = document.createElement('canvas');
    paw.width = r[2] - r[0]; paw.height = r[3] - r[1];
    const c = paw.getContext('2d');
    c.drawImage(limbs, r[0], r[1], paw.width, paw.height, 0, 0, paw.width, paw.height);
    c.globalCompositeOperation = 'destination-in';
    const blend = c.createLinearGradient(0, 0, 0, paw.height * .22);
    blend.addColorStop(0, 'rgba(255,255,255,0)'); blend.addColorStop(1, 'white');
    c.fillStyle = blend; c.fillRect(0, 0, paw.width, paw.height); images[id + 'paw'] = paw;
  };
  limbs.src = '../../app/src/main/assets/pets/rig/' + rigDefinitions[id].limbsImage;
  function add(parent, caption, view) {
    const figure = document.createElement('figure'), canvas = document.createElement('canvas'), text = document.createElement('figcaption');
    canvas.width = 720; canvas.height = 600; canvas.setAttribute('role', 'img'); canvas.setAttribute('aria-label', caption);
    text.textContent = caption; figure.append(canvas, text); document.querySelector(parent).append(figure);
    views.push({id, canvas, view});
  }
  add('#heroes', label, null);
  labels.forEach((label, index) => add('#gallery', (id === 'bear' ? '곰 · ' : '토끼 · ') + label, index));
}
function foot(phase) {
  if (phase < .48) { const t=phase/.48, ease=t*t*(3-2*t); return {travel:1-2*ease,lift:0}; }
  const t=(phase-.48)/.52;
  return {travel:-Math.cos(t*Math.PI),lift:Math.max(0,Math.sin(t*Math.PI))*8};
}
function gesture(type, phase, bear) {
  const g={smile:0,headTilt:0,pawLift:0,pawSide:0,jump:0,rotation:0,squash:0,sparkles:0,earSway:0};
  if (phase <= 0 || phase >= 1) return g;
  const envelope=Math.sin(phase*Math.PI), wave=Math.sin(phase*(bear?4:8)*Math.PI), jumpHeight=bear?14:22;
  if(type==='smile')g.smile=envelope;
  if(type==='wave'){g.smile=envelope*.65;g.pawLift=envelope*(.75+.25*wave);g.pawSide=envelope*wave*(bear?.7:1);}
  if(type==='curious')g.headTilt=(bear?8:12)*envelope*Math.sin(phase*2*Math.PI);
  if(type==='jump'){
    const takeoff=bear?.12:.08,landing=bear?.84:.9,flight=Math.max(0,Math.min(1,(phase-takeoff)/(landing-takeoff)));
    g.smile=envelope;g.jump=jumpHeight*Math.pow(Math.sin(flight*Math.PI),bear?1.5:.85);
    g.squash=(phase<takeoff?Math.sin(phase/takeoff*Math.PI):phase>landing?Math.sin((phase-landing)/(1-landing)*Math.PI):0)*(bear?1:.45);
  }
  if(type==='celebrate'){
    g.smile=envelope;g.pawLift=envelope;g.pawSide=Math.sin(phase*(bear?6:10)*Math.PI)*envelope*(bear?.7:1);
    g.jump=jumpHeight*Math.abs(Math.sin(phase*2*Math.PI))*envelope;g.rotation=(bear?2:3)*Math.sin(phase*4*Math.PI)*envelope;g.sparkles=envelope;
  }
  if(!bear&&['wave','curious','jump','celebrate'].includes(type))g.earSway=envelope*Math.sin(phase*4*Math.PI-Math.PI/3);
  return g;
}
function drawPet(ctx, id, view, idle, phase, g, type) {
  const image=images[id], def=rigDefinitions[id], limbs=images[id+'limbs'], paw=images[id+'paw'];
  if(!image.complete||!image.naturalWidth||!limbs.complete||!limbs.naturalWidth||!paw)return;
  const bear=id==='bear', running=view!==null, v=view??0, side=v===1||v===2, diagonal=v===4||v===5, leftFacing=v===1||v===4, facing=leftFacing?-1:1;
  let rise=0,leftArm=14,rightArm=-14,leftFoot={travel:0,lift:0},rightFoot={travel:0,lift:0};
  if(running){const swing=Math.sin(phase*2*Math.PI)*12;rise=(1-Math.cos(phase*4*Math.PI))*1.2;leftArm+=swing;rightArm-=swing;leftFoot=foot(phase);rightFoot=foot((phase+.5)%1);}
  else{const wave=g.pawLift*72+g.pawSide*8;leftArm+=wave;if(type==='celebrate')rightArm-=wave;}
  const jump=running?0:g.jump;
  function part(index,x,y,w,h){const r=def.parts[index];ctx.drawImage(image,r[0],r[1],r[2]-r[0],r[3]-r[1],x,y,w,h);}
  function limb(index,x,y,w,h){
    if(index===0){ctx.drawImage(paw,x,y,w,h);return;}
    const r=def.limbs[index];ctx.drawImage(limbs,r[0],r[1],r[2]-r[0],r[3]-r[1],x,y,w,h);
  }
  const shadowWidth=66-jump*.55;
  const shadow=ctx.createRadialGradient(180,251,0,180,251,66);shadow.addColorStop(0,'rgba(20,25,20,'+(36-jump*.6)/255+')');shadow.addColorStop(1,'rgba(20,25,20,0)');
  ctx.fillStyle=shadow;ctx.beginPath();ctx.ellipse(180,251,shadowWidth,8.5,0,0,2*Math.PI);ctx.fill();
  ctx.save();ctx.translate(0,-jump);
  [leftFoot,rightFoot].forEach((f,index)=>{
    const spread=side?7:diagonal?20:26,offset=index===0?-spread:spread;
    const x=180+offset+(side?facing*f.travel*13:diagonal?facing*f.travel*7:0);
    const bottom=253-f.lift+(side?0:f.travel*2),w=bear?34:31,h=bear?29:28;
    ctx.save();if(side){ctx.translate(x,bottom-h/2);ctx.rotate(-facing*f.travel*f.lift*.8*Math.PI/180);ctx.translate(-x,-bottom+h/2);}
    limb(1,x-w/2,bottom-h,w,h);ctx.restore();
  });ctx.restore();
  const breath=running?0:Math.sin(idle*2*Math.PI);
  ctx.save();ctx.translate(0,-jump-rise);ctx.translate(180,238);ctx.scale(1+breath*.002+g.squash*.025,1+breath*.005-g.squash*.035);ctx.translate(-180,-238);
  ctx.translate(180,220);ctx.rotate((g.headTilt*.38+g.rotation+g.earSway*.6+(running&&side?facing*3:0))*Math.PI/180);ctx.translate(-180,-220);
  const source=def.parts[v],scale=(bear?188:202)/(def.parts[0][3]-def.parts[0][1]),w=(source[2]-source[0])*scale,h=(source[3]-source[1])*scale,x=180-w/2,y=238-h;
  const spread=side?16:diagonal?(bear?53:46):v===3?(bear?78:66):bear?61:53,shoulderY=bear?191:196;
  function arm(left){let angle=left?leftArm:rightArm,depth=1;
    if(running&&!side){const base=left?14:-14;angle=base+(angle-base)*(diagonal?.7:left?.28:-.28);depth=1-(left?leftFoot:rightFoot).travel*(diagonal?.05:.08);}
    const ax=180+(left?-spread:spread),aw=(bear?31:28)*(1+(depth-1)*.45),length=(bear?24:22)*depth;
    ctx.save();ctx.translate(ax,shoulderY);ctx.rotate(angle*Math.PI/180);limb(0,-aw/2,-6,aw,length+6);ctx.restore();}
  if(v!==0){if(v===3){arm(true);arm(false);}else arm(!leftFacing);}
  part(v,x,y,w,h);
  if(v===0){
    let blink=idle>=.68&&idle<=.72?Math.pow(Math.sin((idle-.68)/.04*Math.PI),2):0;blink=Math.max(blink,Math.min(1,g.smile*3));
    if(blink>0){const b=def.eyeBand;[0,1].forEach(eye=>{ctx.save();ctx.beginPath();const l=eye===0?b[0]:.56,r=eye===0?.44:b[2];ctx.rect(x+w*l,y+h*b[1],w*(r-l),h*(b[3]-b[1]));ctx.clip();ctx.globalAlpha=blink;part(6,x,y,w,h);ctx.restore();});}
    arm(true);arm(false);
  }else if(v!==3)arm(leftFacing);
  ctx.restore();
  if(g.sparkles>0){
    ctx.save();ctx.fillStyle='#ebdba8';ctx.globalAlpha=g.sparkles*220/255;
    [[65,128],[300,98],[286,195]].forEach(([x,y])=>{ctx.save();ctx.translate(x,y-g.sparkles*12);ctx.scale(g.sparkles,g.sparkles);
      ctx.beginPath();[[0,-7],[2,-2],[7,0],[2,2],[0,7],[-2,2],[-7,0],[-2,-2]].forEach(([x,y],i)=>i?ctx.lineTo(x,y):ctx.moveTo(x,y));ctx.closePath();ctx.fill();ctx.restore();});ctx.restore();
  }
}
function tick(time){
  if(lastTime&&playing&&!document.hidden)elapsed+=Math.min(80,time-lastTime);lastTime=time;
  for(const item of views){
    const ctx=item.canvas.getContext('2d');ctx.clearRect(0,0,720,600);ctx.save();ctx.scale(2,2);
    const bear=item.id==='bear',duration=bear?960:820,idle=elapsed%4400/4400,runPhase=selectedPhase??elapsed%duration/duration;
    const type=mode.value,view=item.view??(type==='run'?direction.selectedIndex:null);
    const gestureDuration=({smile:bear?1400:1000,wave:bear?2200:1400,curious:bear?2400:1700,jump:bear?1900:1350,celebrate:bear?3000:2300})[type]||1400;
    const p=selectedPhase??Math.min(1,elapsed%(gestureDuration+700)/gestureDuration);
    const g=item.view===null?gesture(type,p,bear):gesture('idle',0,bear);
    drawPet(ctx,item.id,view,selectedPhase??idle,runPhase,g,type);ctx.restore();
  }requestAnimationFrame(tick);
}
requestAnimationFrame(tick);
