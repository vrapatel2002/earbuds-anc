import struct
def crc16(d):
    c=0xFFFF
    for b in d:
        c^=b
        for _ in range(8): c=(c>>1)^0xA001 if c&1 else c>>1
    return c
def frame(cmd,payload,fsn):
    h=struct.pack("<BHHH",0x55,0x0160,cmd,len(payload))+bytes([fsn])
    return (h+payload+struct.pack("<H",crc16(h+payload))).hex()
T=bytes.fromhex("0300000000010000000000007544c3f5283f020000000000c05a450000803f000000000000000c43cdcc4c3f000000000000000000")
def ef(v,pre=False):
    if pre and v>=0: return bytes([0,0,0,0x80])
    return struct.pack("<f",v)
def ceq(b,m,t):
    g=(m,t,b); buf=bytearray(T); buf[1:5]=ef(-max(g),True)
    for o,x in zip((6,19,32),g): buf[o:o+4]=ef(float(x))
    return bytes(buf)
print("probe", frame(0xC001,b"",1))
print("ancHigh", frame(0xF00F,bytes([1,1,0]),2))
print("ancTransp", frame(0xF00F,bytes([1,7,0]),3))
print("listenRock", frame(0xF01D,bytes([1,0]),4))
print("customEq_b3_m-2_t1", frame(0xF041,ceq(3,-2,1),5))
print("customEq_all-2", ceq(-2,-2,-2).hex())
